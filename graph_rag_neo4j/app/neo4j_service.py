from typing import List, Dict, Any, Optional
from neo4j import GraphDatabase
from app.config import Settings
import logging

logger = logging.getLogger(__name__)


class Neo4jService:
    def __init__(self):
        self.settings = Settings()
        self.driver = GraphDatabase.driver(
            self.settings.neo4j_uri,
            auth=(self.settings.neo4j_user, self.settings.neo4j_password)
        )

    def close(self):
        self.driver.close()

    def create_indexes(self):
        """创建索引优化查询"""
        with self.driver.session() as session:
            session.run("CREATE INDEX IF NOT EXISTS FOR (c:Character) ON (c.name, c.novelId)")
            session.run("CREATE INDEX IF NOT EXISTS FOR (l:Location) ON (l.name, l.novelId)")
            session.run("CREATE INDEX IF NOT EXISTS FOR (o:Organization) ON (o.name, o.novelId)")
            session.run("CREATE INDEX IF NOT EXISTS FOR (e:Event) ON (e.name, e.novelId)")

    def add_entities_and_relations(self, novel_id: str, chapter: int, entities: Dict[str, List[Dict]],
                                   relations: List[Dict]):
        """批量添加实体和关系"""
        with self.driver.session() as session:
            # 1. 添加人物
            for char in entities.get("characters", []):
                session.run("""
                    MERGE (c:Character {name: $name, novelId: $novelId})
                    SET c.aliases = $aliases,
                        c.description = $description,
                        c.first_appearance = CASE WHEN c.first_appearance IS NULL THEN $chapter ELSE c.first_appearance END,
                        c.last_appearance = $chapter
                """, name=char["name"], novelId=novel_id,
                            aliases=char.get("aliases", []),
                            description=char.get("description", ""),
                            chapter=chapter)

            # 2. 添加地点
            for loc in entities.get("locations", []):
                session.run("""
                    MERGE (l:Location {name: $name, novelId: $novelId})
                    SET l.description = $description,
                        l.type = $type
                """, name=loc["name"], novelId=novel_id,
                            description=loc.get("description", ""),
                            type=loc.get("type", "unknown"))

            # 3. 添加组织
            for org in entities.get("organizations", []):
                session.run("""
                    MERGE (o:Organization {name: $name, novelId: $novelId})
                    SET o.description = $description,
                        o.type = $type
                """, name=org["name"], novelId=novel_id,
                            description=org.get("description", ""),
                            type=org.get("type", "unknown"))

            # 4. 添加事件
            for evt in entities.get("events", []):
                session.run("""
                    CREATE (e:Event {name: $name, novelId: $novelId, chapter: $chapter})
                    SET e.description = $description,
                        e.timestamp = $timestamp
                """, name=evt["name"], novelId=novel_id, chapter=chapter,
                            description=evt.get("description", ""),
                            timestamp=evt.get("timestamp", ""))

            # 5. 添加关系
            for rel in relations:
                self._add_relation(session, novel_id, chapter, rel)

    def _add_relation(self, session, novel_id: str, chapter: int, rel: Dict):
        """添加单个关系"""
        rel_type = rel["type"]
        source = rel["source"]
        target = rel["target"]
        properties = rel.get("properties", {})

        query = f"""
            MATCH (a {{name: $source, novelId: $novelId}})
            MATCH (b {{name: $target, novelId: $novelId}})
            MERGE (a)-[r:{rel_type}]->(b)
            SET r.since_chapter = CASE WHEN r.since_chapter IS NULL THEN $chapter ELSE r.since_chapter END,
                r.last_seen = $chapter,
                r += $properties
        """
        session.run(query, source=source, target=target, novelId=novel_id,
                    chapter=chapter, properties=properties)

    def query_character_info(self, novel_id: str, character_name: Optional[str] = None,
                             chapter: Optional[int] = None) -> List[Dict[str, Any]]:
        """查询人物及其关系"""
        with self.driver.session() as session:
            query = """
                MATCH (c:Character {novelId: $novelId})
                WHERE ($charName IS NULL OR c.name = $charName)
                  AND ($chapter IS NULL OR c.first_appearance <= $chapter)
                OPTIONAL MATCH (c)-[r]->(other)
                WHERE $chapter IS NULL OR r.since_chapter <= $chapter
                RETURN c.name as character,
                       c.description as description,
                       c.aliases as aliases,
                       c.first_appearance as first_appearance,
                       type(r) as relation_type,
                       other.name as related_entity,
                       labels(other)[0] as entity_type,
                       r.since_chapter as relation_start
                ORDER BY c.name, relation_type
            """
            result = session.run(query, novelId=novel_id, charName=character_name, chapter=chapter)

            # 整理成结构化数据
            characters = {}
            for record in result:
                char_name = record["character"]
                if char_name not in characters:
                    characters[char_name] = {
                        "name": char_name,
                        "description": record["description"],
                        "aliases": record["aliases"],
                        "first_appearance": record["first_appearance"],
                        "relations": []
                    }

                if record["relation_type"]:
                    characters[char_name]["relations"].append({
                        "type": record["relation_type"],
                        "target": record["related_entity"],
                        "target_type": record["entity_type"],
                        "since_chapter": record["relation_start"]
                    })

            return list(characters.values())

    def query_location_info(self, novel_id: str, location_name: Optional[str] = None) -> List[Dict[str, Any]]:
        """查询地点信息"""
        with self.driver.session() as session:
            query = """
                MATCH (l:Location {novelId: $novelId})
                WHERE $locName IS NULL OR l.name = $locName
                OPTIONAL MATCH (c:Character)-[:LOCATED_IN]->(l)
                RETURN l.name as location,
                       l.description as description,
                       l.type as type,
                       collect(DISTINCT c.name) as characters_present
            """
            result = session.run(query, novelId=novel_id, locName=location_name)
            return [dict(record) for record in result]

    def check_contradictions(self, novel_id: str, chapter: int, new_facts: List[Dict]) -> List[Dict[str, Any]]:
        """检查新内容与已有知识图谱的矛盾"""
        contradictions = []

        with self.driver.session() as session:
            for fact in new_facts:
                fact_type = fact["type"]

                # 检查人物死亡矛盾
                if fact_type == "character_death":
                    char_name = fact["character"]
                    result = session.run("""
                        MATCH (c:Character {name: $name, novelId: $novelId})
                        WHERE c.death_chapter IS NOT NULL AND c.death_chapter < $chapter
                        RETURN c.death_chapter as death_chapter
                    """, name=char_name, novelId=novel_id, chapter=chapter)

                    record = result.single()
                    if record:
                        contradictions.append({
                            "type": "character_already_dead",
                            "character": char_name,
                            "previous_death": record["death_chapter"],
                            "current_chapter": chapter
                        })

                # 检查关系矛盾（例如：已经是敌人但新章节说是朋友）
                elif fact_type == "relationship_change":
                    result = session.run("""
                        MATCH (a:Character {name: $source, novelId: $novelId})
                              -[r]->(b:Character {name: $target})
                        WHERE type(r) = $oldRelType
                        RETURN type(r) as current_relation, r.since_chapter as since
                    """, source=fact["source"], target=fact["target"],
                                         novelId=novel_id, oldRelType=fact["old_relation"])

                    record = result.single()
                    if record and fact["new_relation"] != fact["old_relation"]:
                        contradictions.append({
                            "type": "relationship_conflict",
                            "source": fact["source"],
                            "target": fact["target"],
                            "old_relation": record["current_relation"],
                            "new_relation": fact["new_relation"],
                            "established_chapter": record["since"]
                        })

        return contradictions


neo4j_service = Neo4jService()

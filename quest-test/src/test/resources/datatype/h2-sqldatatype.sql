DROP TABLE IF EXISTS "PeopleName";

CREATE TABLE "PeopleName" (
  "id" int(11) NOT NULL,
  "name" varchar(100) DEFAULT NULL
  PRIMARY KEY ("id")
);

INSERT INTO "PeopleName" VALUES (1,'anton');

DROP TABLE IF EXISTS "PeopleAges";
CREATE TABLE "PeopleAges" (
  "id" varchar(11) NOT NULL,
  "age" int(11) DEFAULT NULL
  PRIMARY KEY ("id")
);

INSERT INTO "PeopleAges" VALUES ('1',24);


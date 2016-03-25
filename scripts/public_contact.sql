-- Table: contact

-- DROP TABLE contact;

CREATE TABLE contact
(
  id bigserial NOT NULL,
  bizname character varying(64),
  industry character varying(64),
  phone character varying(64),
  city character varying(25),
  state character varying(10),
  version integer,
  identifier character varying(36),
  userid bigint,
  CONSTRAINT "XPK_contact_ndx" PRIMARY KEY (id),
  CONSTRAINT "FK_contact_userid" FOREIGN KEY (userid)
      REFERENCES uzer (id) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE
)
WITH (
  OIDS=FALSE
);
ALTER TABLE contact
  OWNER TO biz;
GRANT ALL ON TABLE contact TO biz;

-- Index: "FKI_contact_userid"

-- DROP INDEX "FKI_contact_userid";

CREATE INDEX "FKI_contact_userid"
  ON contact
  USING btree
  (userid);

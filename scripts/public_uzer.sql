-- Table: uzer

-- DROP TABLE uzer;

CREATE TABLE uzer
(
  id bigint NOT NULL DEFAULT nextval('uzer_id_seq'::regclass),
  username character varying(16) NOT NULL,
  role character varying(1) NOT NULL,
  joined_date date NOT NULL,
  password character varying(128) NOT NULL,
  nodata character varying(1) DEFAULT 'Y'::character varying,
  activation character varying(128),
  active_timestamp timestamp with time zone,
  active character varying(1) NOT NULL DEFAULT 'N'::character varying,
  CONSTRAINT "PK_uzers" PRIMARY KEY (id),
  CONSTRAINT "UK_uzers_username" UNIQUE (username)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE uzer
  OWNER TO biz;
GRANT ALL ON TABLE uzer TO biz;

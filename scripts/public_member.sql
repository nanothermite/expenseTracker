-- Table: member

-- DROP TABLE member;

CREATE TABLE member
(
  id bigserial NOT NULL,
  email character varying(255) NOT NULL,
  fname character varying(25) NOT NULL,
  phone_number character varying(12),
  type character varying(1) NOT NULL,
  street1 character varying(64),
  street2 character varying(64),
  city character varying(40),
  state character varying(20),
  country character varying(40) NOT NULL,
  joined_date date NOT NULL,
  ip character varying(16) NOT NULL,
  lname character varying(25) NOT NULL,
  zip character varying(10) NOT NULL,
  userid character varying(16),
  uid bigint,
  CONSTRAINT member_pkey PRIMARY KEY (id),
  CONSTRAINT "FK_member_userid" FOREIGN KEY (uid)
      REFERENCES "user" (id) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT member_email_key UNIQUE (email)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE member
  OWNER TO biz;

-- Index: "FKI_member_userid"

-- DROP INDEX "FKI_member_userid";

CREATE INDEX "FKI_member_userid"
  ON member
  USING btree
  (uid);

-- Table: transactions

-- DROP TABLE transactions;

CREATE TABLE transactions
(
  trandate date NOT NULL,
  acct character varying(10),
  vendor character varying(64),
  description character varying(64),
  phone character varying(64),
  city character varying(25),
  state character varying(10),
  debit double precision NOT NULL,
  credit double precision NOT NULL,
  trantype character varying(30),
  id bigserial NOT NULL,
  contact bigint,
  userid bigint NOT NULL,
  CONSTRAINT "XPK_transactions_ndx" PRIMARY KEY (id),
  CONSTRAINT "FK_transactions_contact" FOREIGN KEY (contact)
      REFERENCES contact (id) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT "FK_transactions_userid" FOREIGN KEY (userid)
      REFERENCES uzer (id) MATCH SIMPLE
      ON UPDATE CASCADE ON DELETE CASCADE
)
WITH (
  OIDS=FALSE
);
ALTER TABLE transactions
  OWNER TO biz;
GRANT ALL ON TABLE transactions TO biz;
GRANT SELECT ON TABLE transactions TO public;

-- Index: "FKI_transactions_contact2"

-- DROP INDEX "FKI_transactions_contact2";

CREATE INDEX "FKI_transactions_contact2"
  ON transactions
  USING btree
  (contact);

-- Index: "FKI_transactions_userid"

-- DROP INDEX "FKI_transactions_userid";

CREATE INDEX "FKI_transactions_userid"
  ON transactions
  USING btree
  (userid);

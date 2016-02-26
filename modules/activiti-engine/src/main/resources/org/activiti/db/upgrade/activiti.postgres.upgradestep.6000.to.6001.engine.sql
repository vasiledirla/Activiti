ALTER TABLE ACT_RU_EXECUTION ADD COLUMN IS_MI_ROOT_ BOOLEAN;

CREATE TABLE ACT_RU_ASYNC_JOB (
  ID_                  VARCHAR(64)  NOT NULL,
  REV_                 INTEGER,
  TYPE_                VARCHAR(255) NOT NULL,
  LOCK_EXP_TIME_       TIMESTAMP,
  LOCK_OWNER_          VARCHAR(255),
  EXCLUSIVE_           BOOLEAN,
  EXECUTION_ID_        VARCHAR(64),
  PROCESS_INSTANCE_ID_ VARCHAR(64),
  PROC_DEF_ID_         VARCHAR(64),
  RETRIES_             INTEGER,
  EXCEPTION_STACK_ID_  VARCHAR(64),
  EXCEPTION_MSG_       VARCHAR(4000),
  HANDLER_TYPE_        VARCHAR(255),
  HANDLER_CFG_         VARCHAR(4000),
  TENANT_ID_           VARCHAR(255) DEFAULT '',
  PRIMARY KEY (ID_)
);

CREATE TABLE ACT_RU_TIMER_JOB (
  ID_                  VARCHAR(64)  NOT NULL,
  REV_                 INTEGER,
  TYPE_                VARCHAR(255) NOT NULL,
  LOCK_EXP_TIME_       TIMESTAMP,
  LOCK_OWNER_          VARCHAR(255),
  EXCLUSIVE_           BOOLEAN,
  EXECUTION_ID_        VARCHAR(64),
  PROCESS_INSTANCE_ID_ VARCHAR(64),
  PROC_DEF_ID_         VARCHAR(64),
  RETRIES_             INTEGER,
  EXCEPTION_STACK_ID_  VARCHAR(64),
  EXCEPTION_MSG_       VARCHAR(4000),
  DUEDATE_             TIMESTAMP,
  REPEAT_              VARCHAR(255),
  HANDLER_TYPE_        VARCHAR(255),
  HANDLER_CFG_         VARCHAR(4000),
  TENANT_ID_           VARCHAR(255) DEFAULT '',
  SUSPENSION_STATE_    INTEGER,
  PRIMARY KEY (ID_)
);

CREATE INDEX ACT_IDX_ASYNC_JOB_EXCEPTION ON ACT_RU_ASYNC_JOB (EXCEPTION_STACK_ID_);
ALTER TABLE ACT_RU_ASYNC_JOB
ADD CONSTRAINT ACT_FK_ASYNC_JOB_EXCEPTION
FOREIGN KEY (EXCEPTION_STACK_ID_)
REFERENCES ACT_GE_BYTEARRAY (ID_);

CREATE INDEX ACT_IDX_TIMER_JOB_EXCEPTION ON ACT_RU_TIMER_JOB (EXCEPTION_STACK_ID_);
ALTER TABLE ACT_RU_TIMER_JOB
ADD CONSTRAINT ACT_FK_TIMER_JOB_EXCEPTION
FOREIGN KEY (EXCEPTION_STACK_ID_)
REFERENCES ACT_GE_BYTEARRAY (ID_);

-- Migrate data from old job table to the new format
INSERT INTO ACT_RU_ASYNC_JOB (SELECT
                                ID_,
                                REV_,
                                TYPE_,
                                GREATEST(DUEDATE_, LOCK_EXP_TIME_) AS LOCK_EXP_TIME_,
                                LOCK_OWNER_,
                                EXCLUSIVE_,
                                EXECUTION_ID_,
                                PROCESS_INSTANCE_ID_,
                                PROC_DEF_ID_,
                                RETRIES_,
                                EXCEPTION_STACK_ID_,
                                EXCEPTION_MSG_,
                                HANDLER_TYPE_,
                                HANDLER_CFG_,
                                TENANT_ID_,
                                NULL
                              FROM ACT_RU_JOB
                              WHERE TYPE_ LIKE 'message');

INSERT INTO ACT_RU_TIMER_JOB (SELECT
                                ID_,
                                REV_,
                                TYPE_,
                                LOCK_EXP_TIME_,
                                LOCK_OWNER_,
                                EXCLUSIVE_,
                                EXECUTION_ID_,
                                PROCESS_INSTANCE_ID_,
                                PROC_DEF_ID_,
                                RETRIES_,
                                EXCEPTION_STACK_ID_,
                                EXCEPTION_MSG_,
                                DUEDATE_,
                                REPEAT_,
                                HANDLER_TYPE_,
                                HANDLER_CFG_,
                                TENANT_ID_,
                                NULL
                              FROM ACT_RU_JOB
                              WHERE TYPE_ LIKE 'timer');

-- Delete old table
ALTER TABLE ACT_RU_JOB
DROP foreign KEY ACT_FK_JOB_EXCEPTION;

DROP TABLE ACT_RU_JOB;

UPDATE ACT_GE_PROPERTY
SET VALUE_ = '6.0.0.1'
WHERE NAME_ = 'schema.version';

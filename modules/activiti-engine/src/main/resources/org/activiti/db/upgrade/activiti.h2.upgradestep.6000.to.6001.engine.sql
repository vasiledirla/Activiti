ALTER TABLE ACT_RU_EXECUTION ADD column IS_MI_ROOT_ BIT;

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
  SUSPENSION_STATE_    INTEGER,
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

ALTER TABLE ACT_RU_ASYNC_JOB
ADD CONSTRAINT ACT_FK_ASYNC_JOB_EXCEPTION
FOREIGN KEY (EXCEPTION_STACK_ID_)
REFERENCES ACT_GE_BYTEARRAY;


ALTER TABLE ACT_RU_TIMER_JOB
ADD CONSTRAINT ACT_FK_TIMER_JOB_EXCEPTION
FOREIGN KEY (EXCEPTION_STACK_ID_)
REFERENCES ACT_GE_BYTEARRAY;

-- Migrate data from old job table to the new format
INSERT INTO ACT_RU_ASYNC_JOB (SELECT
                                JOB.ID_,
                                JOB.REV_,
                                JOB.TYPE_,
                                GREATEST(JOB.DUEDATE_, JOB.LOCK_EXP_TIME_) AS LOCK_EXP_TIME_,
                                JOB.LOCK_OWNER_,
                                JOB.EXCLUSIVE_,
                                JOB.EXECUTION_ID_,
                                JOB.PROCESS_INSTANCE_ID_,
                                JOB.PROC_DEF_ID_,
                                JOB.RETRIES_,
                                JOB.EXCEPTION_STACK_ID_,
                                JOB.EXCEPTION_MSG_,
                                JOB.HANDLER_TYPE_,
                                JOB.HANDLER_CFG_,
                                JOB.TENANT_ID_,
                                PI.SUSPENSION_STATE_
                                FROM ACT_RU_JOB JOB left JOIN ACT_RU_EXECUTION PI
                                on pi.ID_ = JOB.PROCESS_INSTANCE_ID_
                              WHERE JOB.TYPE_ LIKE 'message');

INSERT INTO ACT_RU_TIMER_JOB (SELECT
                                JOB.ID_,
                                JOB.REV_,
                                JOB.TYPE_,
                                JOB.LOCK_EXP_TIME_,
                                JOB.LOCK_OWNER_,
                                JOB.EXCLUSIVE_,
                                JOB.EXECUTION_ID_,
                                JOB.PROCESS_INSTANCE_ID_,
                                JOB.PROC_DEF_ID_,
                                JOB.RETRIES_,
                                JOB.EXCEPTION_STACK_ID_,
                                JOB.EXCEPTION_MSG_,
                                JOB.DUEDATE_,
                                JOB.REPEAT_,
                                JOB.HANDLER_TYPE_,
                                JOB.HANDLER_CFG_,
                                JOB.TENANT_ID_,
                                PI.SUSPENSION_STATE_
                                FROM ACT_RU_JOB JOB left JOIN ACT_RU_EXECUTION PI
                                on pi.ID_ = JOB.PROCESS_INSTANCE_ID_
                                 WHERE JOB.TYPE_ LIKE 'timer');

-- Delete old table
ALTER TABLE ACT_RU_JOB
DROP FOREIGN KEY ACT_FK_JOB_EXCEPTION;

DROP TABLE ACT_RU_JOB;

UPDATE ACT_GE_PROPERTY
SET VALUE_ = '6.0.0.1'
WHERE NAME_ = 'schema.version';

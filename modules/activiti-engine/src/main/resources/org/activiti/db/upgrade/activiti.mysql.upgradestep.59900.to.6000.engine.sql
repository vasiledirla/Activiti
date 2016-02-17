alter table ACT_RE_PROCDEF add column ENGINE_VERSION_ varchar(255);
update ACT_RE_PROCDEF set ENGINE_VERSION_ = 'activiti-5';

alter table ACT_RE_DEPLOYMENT add column ENGINE_VERSION_ varchar(255);
update ACT_RE_DEPLOYMENT set ENGINE_VERSION_ = 'activiti-5';

alter table ACT_RU_EXECUTION add column ROOT_PROC_INST_ID_ varchar(64);
create index ACT_IDX_EXEC_ROOT on ACT_RU_EXECUTION(ROOT_PROC_INST_ID_);

alter table ACT_RU_EXECUTION  drop foreign key ACT_FK_EXE_PARENT;
alter table ACT_RU_EXECUTION add constraint ACT_FK_EXE_PARENT foreign key (PARENT_ID_) references ACT_RU_EXECUTION (ID_) on delete cascade; 

alter table ACT_RU_EXECUTION drop foreign key ACT_FK_EXE_SUPER;
alter table ACT_RU_EXECUTION add constraint ACT_FK_EXE_SUPER foreign key (SUPER_EXEC_) references ACT_RU_EXECUTION (ID_) on delete cascade;

create table ACT_RU_ASYNC_JOB (
    ID_ varchar(64) not null,
    REV_ integer,
    TYPE_ varchar(255) not null,
    LOCK_EXP_TIME_ timestamp,
    LOCK_OWNER_ varchar(255),
    EXCLUSIVE_ smallint check(EXCLUSIVE_ in (1,0)),
    EXECUTION_ID_ varchar(64),
    PROCESS_INSTANCE_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    RETRIES_ integer,
    EXCEPTION_STACK_ID_ varchar(64),
    EXCEPTION_MSG_ varchar(4000),
    HANDLER_TYPE_ varchar(255),
    HANDLER_CFG_ varchar(4000),
    TENANT_ID_ varchar(255) default '',
    SUSPENSION_STATE_ int(11),
    primary key (ID_)
);

create index ACT_IDX_ASYNC_JOB_EXCEPTION_STACK_ID on ACT_RU_ASYNC_JOB(EXCEPTION_STACK_ID_);

alter table ACT_RU_ASYNC_JOB
    add constraint ACT_FK_ASYNC_JOB_EXCEPTION
    foreign key (EXCEPTION_STACK_ID_)
    references ACT_GE_BYTEARRAY (ID_);


  create table ACT_RU_TIMER_JOB (
    ID_ varchar(64) not null,
    REV_ integer,
    TYPE_ varchar(255) not null,
    LOCK_EXP_TIME_ timestamp,
    LOCK_OWNER_ varchar(255),
    EXCLUSIVE_ smallint check(EXCLUSIVE_ in (1,0)),
    EXECUTION_ID_ varchar(64),
    PROCESS_INSTANCE_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    RETRIES_ integer,
    EXCEPTION_STACK_ID_ varchar(64),
    EXCEPTION_MSG_ varchar(4000),
    DUEDATE_ timestamp,
    REPEAT_ varchar(255),
    HANDLER_TYPE_ varchar(255),
    HANDLER_CFG_ varchar(4000),
    TENANT_ID_ varchar(255) default '',
    SUSPENSION_STATE_ int(11),
    primary key (ID_)
);

create index ACT_IDX_TIMER_JOB_EXCEPTION_STACK_ID on ACT_RU_TIMER_JOB(EXCEPTION_STACK_ID_);

alter table ACT_RU_TIMER_JOB
    add constraint ACT_FK_TIMER_JOB_EXCEPTION
    foreign key (EXCEPTION_STACK_ID_)
    references ACT_GE_BYTEARRAY (ID_);


update ACT_GE_PROPERTY set VALUE_ = '6.0.0.0' where NAME_ = 'schema.version';

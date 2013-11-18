drop table if exists ss_task;
drop table if exists ss_user;
drop table if exists image_info;

create table image_info (
	id bigint auto_increment,
	title varchar(255) not null,
	location varchar(255) not null,
	tags varchar(255) not null,
	fileName varchar(255) not null,
	update_date timestamp not null,
	primary key (id)
) engine=InnoDB;

create table ss_task (
	id bigint auto_increment,
	title varchar(128) not null,
	description varchar(255),
	user_id bigint not null,
    primary key (id)
) engine=InnoDB;

create table ss_user (
	id bigint auto_increment,
	login_name varchar(64) not null unique,
	name varchar(64) not null,
	password varchar(255) not null,
	salt varchar(64) not null,
	roles varchar(255) not null,
	register_date timestamp not null default 0,
	primary key (id)
) engine=InnoDB;
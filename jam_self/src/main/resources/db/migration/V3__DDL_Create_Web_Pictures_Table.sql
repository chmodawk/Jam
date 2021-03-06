-- auto-generated definition
create table web_pictures
(
    pid         bigint  default '-1'::integer not null
        constraint web_pictures_pk
            primary key,
    uid         bigint  default '-1'::integer not null,
    title       varchar    default '无标题'::text   not null,
    author      varchar    default '未知作者'::text  not null,
    url         varchar    default ''::text      not null,
    is_r18      boolean default false         not null,
    width       integer default '-1'::integer not null,
    height      integer default '-1'::integer not null,
    tags        varchar    default ''::text      not null,
    base64_data varchar
);

create unique index web_pictures_pid_uindex
    on web_pictures (pid);


create table if not exists users (
  id varchar(32) primary key,
  name varchar(80) not null,
  phone varchar(20) not null unique,
  major varchar(120) not null,
  avatar varchar(16) not null,
  presence varchar(20) not null,
  created_at timestamp not null default current_timestamp
);

create table if not exists verification_codes (
  phone varchar(20) primary key,
  code varchar(8) not null,
  updated_at timestamp not null default current_timestamp on update current_timestamp
);

create table if not exists auth_sessions (
  token varchar(128) primary key,
  user_id varchar(32) not null,
  created_at timestamp not null default current_timestamp,
  last_seen_at timestamp not null default current_timestamp on update current_timestamp
);

create table if not exists friendships (
  id bigint primary key auto_increment,
  first_user_id varchar(32) not null,
  second_user_id varchar(32) not null,
  created_at timestamp not null default current_timestamp,
  unique key uk_friendship_pair (first_user_id, second_user_id)
);

create table if not exists friend_requests (
  id varchar(32) primary key,
  from_user_id varchar(32) not null,
  to_user_id varchar(32) not null,
  status varchar(20) not null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp on update current_timestamp
);

create table if not exists messages (
  id varchar(32) primary key,
  peer_id varchar(32) not null,
  from_user_id varchar(32) not null,
  body varchar(1000) not null,
  status varchar(20) not null,
  created_at timestamp not null default current_timestamp
);

create table if not exists message_attachments (
  id varchar(32) primary key,
  message_id varchar(32) not null,
  file_name varchar(255) not null,
  file_size bigint not null,
  mime_type varchar(120) not null,
  display_kind varchar(40) not null
);

create table if not exists posts (
  id varchar(32) primary key,
  author_id varchar(32) not null,
  body varchar(2000) not null,
  likes int not null default 0,
  moderation_status varchar(20) not null,
  created_at timestamp not null default current_timestamp
);

create table if not exists comments (
  id varchar(32) primary key,
  post_id varchar(32) not null,
  author_id varchar(32) not null,
  body varchar(1000) not null,
  moderation_status varchar(20) not null,
  created_at timestamp not null default current_timestamp
);

create table if not exists moderation_items (
  id varchar(32) primary key,
  content_type varchar(30) not null,
  content_id varchar(32) not null,
  status varchar(20) not null,
  reason varchar(255) not null,
  created_at timestamp not null default current_timestamp
);

create table if not exists audit_events (
  id varchar(32) primary key,
  actor_id varchar(32) not null,
  action varchar(80) not null,
  target varchar(120) not null,
  created_at timestamp not null default current_timestamp
);

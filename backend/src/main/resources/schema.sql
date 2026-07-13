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

create table if not exists conversation_reads (
  user_id varchar(32) not null,
  peer_id varchar(32) not null,
  last_read_message_id varchar(32) not null,
  updated_at timestamp not null default current_timestamp on update current_timestamp,
  primary key (user_id, peer_id)
);

create table if not exists posts (
  id varchar(32) primary key,
  author_id varchar(32) not null,
  body varchar(2000) not null,
  visibility varchar(20) not null default '全校可见',
  likes int not null default 0,
  moderation_status varchar(20) not null,
  created_at timestamp not null default current_timestamp
);

set @post_visibility_exists = (
  select count(*)
  from information_schema.columns
  where table_schema = database()
    and table_name = 'posts'
    and column_name = 'visibility'
);
set @post_visibility_migration = if(
  @post_visibility_exists = 0,
  'alter table posts add column visibility varchar(20) not null default ''全校可见''',
  'select 1'
);
prepare add_post_visibility from @post_visibility_migration;
execute add_post_visibility;
deallocate prepare add_post_visibility;

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

create table if not exists activities (
  id varchar(32) primary key,
  title varchar(120) not null,
  description varchar(2000) not null,
  category varchar(60) not null,
  location varchar(160) not null,
  starts_at datetime not null,
  ends_at datetime not null,
  capacity int not null,
  organizer_id varchar(32) not null,
  status varchar(20) not null,
  review_decision varchar(20) not null,
  review_reason varchar(500) null,
  reviewed_by varchar(32) null,
  reviewed_at datetime null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp on update current_timestamp,
  key idx_activities_status_start (status, starts_at),
  key idx_activities_review_queue (review_decision, created_at),
  key idx_activities_organizer (organizer_id, created_at)
);

create table if not exists activity_reviews (
  id varchar(32) primary key,
  activity_id varchar(32) not null,
  actor_id varchar(32) not null,
  decision varchar(20) not null,
  reason varchar(500) null,
  created_at timestamp(6) not null default current_timestamp(6),
  key idx_activity_reviews_activity (activity_id, created_at)
);

set @activity_review_time_precision = (
  select coalesce(datetime_precision, 0)
  from information_schema.columns
  where table_schema = database()
    and table_name = 'activity_reviews'
    and column_name = 'created_at'
);
set @activity_review_time_migration = if(
  @activity_review_time_precision = 6,
  'select 1',
  'alter table activity_reviews modify created_at timestamp(6) not null default current_timestamp(6)'
);
prepare update_activity_review_time from @activity_review_time_migration;
execute update_activity_review_time;
deallocate prepare update_activity_review_time;

create table if not exists activity_registrations (
  id varchar(32) primary key,
  activity_id varchar(32) not null,
  attendee_id varchar(32) not null,
  status varchar(20) not null,
  registered_at datetime null,
  waitlisted_at datetime null,
  cancelled_at datetime null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp on update current_timestamp,
  unique key uk_activity_registrations_attendee (activity_id, attendee_id),
  key idx_activity_registrations_status (activity_id, status),
  key idx_activity_registrations_waitlist (activity_id, status, waitlisted_at, id),
  key idx_activity_registrations_attendee (attendee_id, updated_at)
);

create table if not exists activity_registration_events (
  id varchar(32) primary key,
  registration_id varchar(32) not null,
  activity_id varchar(32) not null,
  attendee_id varchar(32) not null,
  actor_id varchar(32) not null,
  event_type varchar(20) not null,
  from_status varchar(20) null,
  to_status varchar(20) not null,
  created_at timestamp(6) not null default current_timestamp(6),
  key idx_activity_registration_events_activity (activity_id, created_at),
  key idx_activity_registration_events_registration (registration_id, created_at)
);

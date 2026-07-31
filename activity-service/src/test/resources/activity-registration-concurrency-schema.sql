create table activities (
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
  updated_at timestamp not null default current_timestamp on update current_timestamp
);

create table activity_registrations (
  id varchar(32) primary key,
  activity_id varchar(32) not null,
  attendee_id varchar(32) not null,
  status varchar(20) not null,
  registered_at datetime null,
  waitlisted_at datetime null,
  checked_in_at datetime(6) null,
  cancelled_at datetime null,
  created_at timestamp not null default current_timestamp,
  updated_at timestamp not null default current_timestamp on update current_timestamp,
  unique key uk_activity_registrations_attendee (activity_id, attendee_id),
  key idx_activity_registrations_status (activity_id, status),
  key idx_activity_registrations_waitlist (activity_id, status, waitlisted_at, id)
);

create table activity_registration_events (
  id varchar(32) primary key,
  registration_id varchar(32) not null,
  activity_id varchar(32) not null,
  attendee_id varchar(32) not null,
  actor_id varchar(32) not null,
  event_type varchar(20) not null,
  from_status varchar(20) null,
  to_status varchar(20) not null,
  created_at timestamp(6) not null default current_timestamp(6)
);

create table outbox_events (
  id varchar(64) primary key,
  aggregate_type varchar(80) not null,
  aggregate_id varchar(64) not null,
  event_type varchar(120) not null,
  payload json not null,
  status varchar(20) not null,
  attempts int not null default 0,
  next_attempt_at datetime(6) not null,
  published_at datetime(6) null,
  last_error varchar(1000) null,
  created_at timestamp(6) not null default current_timestamp(6)
);

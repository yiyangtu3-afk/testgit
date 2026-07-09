delete from friend_requests
where from_user_id in ('u-alice', 'u-ben', 'u-cora', 'u-admin')
   or to_user_id in ('u-alice', 'u-ben', 'u-cora', 'u-admin');

delete from friendships
where first_user_id in ('u-alice', 'u-ben', 'u-cora', 'u-admin')
   or second_user_id in ('u-alice', 'u-ben', 'u-cora', 'u-admin');

delete from users
where id in ('u-alice', 'u-ben', 'u-cora', 'u-admin');

insert into users (id, name, phone, major, avatar, presence) values
  ('u-1001', '林一', '13800000001', '学生账号', '林', 'online'),
  ('u-2001', '陈老师', '13800000002', '教师', '陈', 'online'),
  ('u-2002', '周同学', '13800000003', '学生', '周', 'offline'),
  ('u-2003', '教务管理员', '13800000004', '管理员', '管', 'online')
on duplicate key update
  name = values(name),
  phone = values(phone),
  major = values(major),
  avatar = values(avatar),
  presence = values(presence);

insert ignore into friendships (first_user_id, second_user_id) values
  ('u-1001', 'u-2001'),
  ('u-1001', 'u-2003');

delete from friend_requests
where exists (
  select 1
  from friendships
  where (first_user_id = friend_requests.from_user_id and second_user_id = friend_requests.to_user_id)
     or (first_user_id = friend_requests.to_user_id and second_user_id = friend_requests.from_user_id)
);

insert into friend_requests (id, from_user_id, to_user_id, status)
select 'fr-1001', 'u-2002', 'u-1001', 'pending'
where not exists (
  select 1
  from friendships
  where (first_user_id = 'u-1001' and second_user_id = 'u-2002')
     or (first_user_id = 'u-2002' and second_user_id = 'u-1001')
)
on duplicate key update
  from_user_id = values(from_user_id),
  to_user_id = values(to_user_id),
  status = values(status);

insert into messages (id, peer_id, from_user_id, body, status) values
  ('1', 'u-1001', 'u-2001', '毕设题目可以再收敛一点，先把第一版 demo 跑起来。', 'active'),
  ('2', 'u-2001', 'u-1001', '收到，我先做登录、好友和单聊。', 'active'),
  ('3', 'u-1001', 'u-2002', '晚上一起测试聊天功能？', 'active'),
  ('4', 'u-1001', 'u-2003', '后台需要能看到基础统计和审计记录。', 'active')
on duplicate key update
  peer_id = values(peer_id),
  from_user_id = values(from_user_id),
  body = values(body),
  status = values(status);

insert into message_attachments (id, message_id, file_name, file_size, mime_type, display_kind) values
  ('att-seed-1', '2', 'campuslink-demo-notes.txt', 2048, 'text/plain', 'document')
on duplicate key update
  message_id = values(message_id),
  file_name = values(file_name),
  file_size = values(file_size),
  mime_type = values(mime_type),
  display_kind = values(display_kind);

delete from comments where post_id in ('p-campus-run', 'p-library-seat');
delete from posts where id in ('p-campus-run', 'p-library-seat');

insert into posts (id, author_id, body, likes, moderation_status) values
  ('1', 'u-2001', '软件工程课程设计进入验收阶段，请各组准备可运行 demo、架构图和数据库设计。', 12, 'approved'),
  ('2', 'u-1001', 'CampusLink 第一条链路：验证码登录、好友搜索、聊天和动态已经串起来。', 5, 'approved'),
  ('9001', 'u-2002', '今天社团招新摊位安排在图书馆门口，欢迎大家下午来看看。', 0, 'pending')
on duplicate key update
  author_id = values(author_id),
  body = values(body),
  likes = values(likes),
  moderation_status = values(moderation_status);

insert into comments (id, post_id, author_id, body, moderation_status) values
  ('1', '1', 'u-2002', '收到，我把验收材料清单也补上。', 'approved'),
  ('2', '1', 'u-1001', 'demo 已经可以本地验证，晚点同步说明。', 'approved'),
  ('3', '2', 'u-2001', '很好，下一步可以补评论和后台审计链路。', 'approved')
on duplicate key update
  post_id = values(post_id),
  author_id = values(author_id),
  body = values(body),
  moderation_status = values(moderation_status);

delete from moderation_items
where id in ('m-post-1', 'm-comment-1')
   or content_id in ('p-campus-run', 'c-library-1');

insert into moderation_items (id, content_type, content_id, status, reason) values
  ('m-demo-post-9001', 'post', '9001', 'pending', '校园动态发布审核')
on duplicate key update
  content_type = values(content_type),
  content_id = values(content_id),
  status = values(status),
  reason = values(reason);

insert into audit_events (id, actor_id, action, target) values
  ('a-login-1', 'u-1001', '用户', '13800000001 登录成功'),
  ('a-report-1', 'u-2003', '后台', '教务管理员查看后台报表')
on duplicate key update
  actor_id = values(actor_id),
  action = values(action),
  target = values(target);

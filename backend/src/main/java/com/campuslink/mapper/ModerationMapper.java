package com.campuslink.mapper;

import com.campuslink.entity.DemoEntities.ModerationItemEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ModerationMapper {

  String MODERATION_SELECT = """
      select m.id,
             m.content_type as type,
             cast(m.content_id as signed) as targetId,
             case
               when m.content_type = 'comment' then cast(c.post_id as signed)
               else null
             end as postId,
             case
               when m.content_type = 'post' then concat('动态：', left(coalesce(p.body, ''), 24))
               when m.content_type = 'comment' then concat('评论：', left(coalesce(c.body, ''), 24))
               else '待审核内容'
             end as title,
             coalesce(post_author.name, comment_author.name, '演示用户') as author,
             coalesce(p.body, c.body, '') as body,
             m.status,
             m.reason,
             date_format(m.created_at, '%Y-%m-%d %H:%i') as submittedAt,
             date_format(m.created_at, '%H:%i') as time
      from moderation_items m
      left join posts p on m.content_type = 'post' and p.id = m.content_id
      left join users post_author on post_author.id = p.author_id
      left join comments c on m.content_type = 'comment' and c.id = m.content_id
      left join users comment_author on comment_author.id = c.author_id
      """;

  @Select(MODERATION_SELECT + " order by m.created_at desc, m.id desc")
  List<ModerationItemEntity> findAll();

  @Select(MODERATION_SELECT + " where m.status = 'pending' order by m.created_at desc, m.id desc")
  List<ModerationItemEntity> findPending();

  @Select("select count(*) from moderation_items where status = 'pending'")
  long countPending();

  @Insert("""
      insert into moderation_items (id, content_type, content_id, status, reason)
      values (#{id}, #{type}, #{targetId}, 'pending', #{reason})
      """)
  void insert(
      @Param("id") String id,
      @Param("type") String type,
      @Param("targetId") String targetId,
      @Param("reason") String reason);

  @Select(MODERATION_SELECT + " where m.id = #{itemId}")
  ModerationItemEntity findById(@Param("itemId") String itemId);

  @Update("update moderation_items set status = #{status} where id = #{itemId}")
  void updateStatus(@Param("itemId") String itemId, @Param("status") String status);

  @Delete("""
      <script>
      delete from moderation_items
      where id in
      <foreach collection="itemIds" item="itemId" open="(" separator="," close=")">
        #{itemId}
      </foreach>
      </script>
      """)
  int deleteByIds(@Param("itemIds") List<String> itemIds);
}

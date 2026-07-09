package com.campuslink.mapper;

import com.campuslink.entity.DemoEntities.AuditEventEntity;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AuditMapper {

  @Insert("""
      insert into audit_events (id, actor_id, action, target)
      values (#{id}, 'u-2003', #{module}, #{event})
      """)
  void insert(@Param("id") String id, @Param("module") String module, @Param("event") String event);

  @Select("""
      select id,
             date_format(created_at, '%H:%i') as time,
             action as module,
             target as event
      from audit_events
      order by created_at desc, id desc
      limit #{limit}
      """)
  List<AuditEventEntity> findRecent(@Param("limit") int limit);

  @Select("select count(*) from audit_events")
  int count();

  @Delete("""
      <script>
      delete from audit_events
      where id in
      <foreach collection="eventIds" item="eventId" open="(" separator="," close=")">
        #{eventId}
      </foreach>
      </script>
      """)
  int deleteByIds(@Param("eventIds") List<String> eventIds);
}

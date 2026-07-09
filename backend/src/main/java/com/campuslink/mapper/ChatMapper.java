package com.campuslink.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ChatMapper {

  @Select("""
      select cast(id as signed) as id,
             from_user_id as fromUserId,
             body,
             status,
             date_format(created_at, '%H:%i') as time
      from messages
      where (peer_id = #{peerId} and from_user_id = #{currentUserId})
         or (peer_id = #{currentUserId} and from_user_id = #{peerId})
      order by created_at, cast(id as signed)
      """)
  List<MessageRow> findMessages(@Param("peerId") String peerId, @Param("currentUserId") String currentUserId);

  @Select("""
      select cast(id as signed) as id,
             from_user_id as fromUserId,
             body,
             status,
             date_format(created_at, '%H:%i') as time
      from messages
      where id = #{messageId}
        and (
          (peer_id = #{peerId} and from_user_id = #{currentUserId})
          or (peer_id = #{currentUserId} and from_user_id = #{peerId})
        )
      """)
  MessageRow findMessage(
      @Param("peerId") String peerId,
      @Param("currentUserId") String currentUserId,
      @Param("messageId") String messageId);

  @Insert("""
      insert into messages (id, peer_id, from_user_id, body, status)
      values (#{id}, #{peerId}, #{fromUserId}, #{body}, 'active')
      """)
  void insertMessage(
      @Param("id") String id,
      @Param("peerId") String peerId,
      @Param("fromUserId") String fromUserId,
      @Param("body") String body);

  @Insert("""
      insert into message_attachments (id, message_id, file_name, file_size, mime_type, display_kind)
      values (#{id}, #{messageId}, #{fileName}, #{fileSize}, #{mimeType}, #{displayKind})
      """)
  void insertAttachment(
      @Param("id") String id,
      @Param("messageId") String messageId,
      @Param("fileName") String fileName,
      @Param("fileSize") long fileSize,
      @Param("mimeType") String mimeType,
      @Param("displayKind") String displayKind);

  @Select("""
      select id,
             file_name as fileName,
             file_size as fileSize,
             mime_type as mimeType,
             display_kind as displayKind
      from message_attachments
      where message_id = #{messageId}
      order by id
      """)
  List<AttachmentRow> findAttachments(@Param("messageId") String messageId);

  @Update("""
      update messages
      set status = 'withdrawn'
      where id = #{messageId}
        and (
          (peer_id = #{peerId} and from_user_id = #{currentUserId})
          or (peer_id = #{currentUserId} and from_user_id = #{peerId})
        )
      """)
  void withdrawMessage(
      @Param("peerId") String peerId,
      @Param("currentUserId") String currentUserId,
      @Param("messageId") String messageId);

  record MessageRow(Long id, String fromUserId, String body, String status, String time) {
  }

  record AttachmentRow(String id, String fileName, long fileSize, String mimeType, String displayKind) {
  }
}

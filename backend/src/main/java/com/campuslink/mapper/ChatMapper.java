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
      where (
            (peer_id = #{peerId} and from_user_id = #{currentUserId})
         or (peer_id = #{currentUserId} and from_user_id = #{peerId})
      )
        and (#{beforeId} is null or cast(id as signed) < #{beforeId})
      order by created_at desc, cast(id as signed) desc
      limit #{limit}
      """)
  List<MessageRow> findMessagePage(
      @Param("peerId") String peerId,
      @Param("currentUserId") String currentUserId,
      @Param("beforeId") Long beforeId,
      @Param("limit") int limit);

  @Insert("""
      insert into conversation_reads (user_id, peer_id, last_read_message_id)
      values (#{userId}, #{peerId}, #{lastReadMessageId})
      on duplicate key update
        last_read_message_id = values(last_read_message_id),
        updated_at = current_timestamp
      """)
  void upsertConversationRead(
      @Param("userId") String userId,
      @Param("peerId") String peerId,
      @Param("lastReadMessageId") String lastReadMessageId);

  @Select("""
      select m.from_user_id as peerId, count(*) as count
      from messages m
      left join conversation_reads r
        on r.user_id = #{userId} and r.peer_id = m.from_user_id
      where m.peer_id = #{userId}
        and m.from_user_id <> #{userId}
        and m.status = 'active'
        and m.id regexp '^[0-9]+$'
        and exists (
          select 1
          from friendships f
          where (f.first_user_id = #{userId} and f.second_user_id = m.from_user_id)
             or (f.second_user_id = #{userId} and f.first_user_id = m.from_user_id)
        )
        and (r.last_read_message_id is null or cast(m.id as signed) > cast(r.last_read_message_id as signed))
      group by m.from_user_id
      """)
  List<UnreadCountRow> unreadCounts(@Param("userId") String userId);

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
      insert into message_attachments (
        id, message_id, file_name, file_size, mime_type, display_kind, content
      )
      values (
        #{id}, #{messageId}, #{fileName}, #{fileSize}, #{mimeType}, #{displayKind}, #{content}
      )
      """)
  void insertAttachment(
      @Param("id") String id,
      @Param("messageId") String messageId,
      @Param("fileName") String fileName,
      @Param("fileSize") long fileSize,
      @Param("mimeType") String mimeType,
      @Param("displayKind") String displayKind,
      @Param("content") byte[] content);

  @Select("""
      select id,
             file_name as fileName,
             file_size as fileSize,
             mime_type as mimeType,
             display_kind as displayKind,
             content is not null as hasContent
      from message_attachments
      where message_id = #{messageId}
      order by id
      """)
  List<AttachmentRow> findAttachments(@Param("messageId") String messageId);

  @Select("""
      select a.file_name as fileName,
             a.mime_type as mimeType,
             a.content
      from message_attachments a
      join messages m on m.id = a.message_id
      where a.id = #{attachmentId}
        and m.status = 'active'
        and (
          (m.peer_id = #{peerId} and m.from_user_id = #{currentUserId})
          or (m.peer_id = #{currentUserId} and m.from_user_id = #{peerId})
        )
      """)
  AttachmentContentRow findAttachmentContent(
      @Param("peerId") String peerId,
      @Param("currentUserId") String currentUserId,
      @Param("attachmentId") String attachmentId);

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

  record AttachmentRow(
      String id,
      String fileName,
      long fileSize,
      String mimeType,
      String displayKind,
      boolean hasContent) {
  }

  record AttachmentContentRow(String fileName, String mimeType, byte[] content) {
  }

  record UnreadCountRow(String peerId, int count) {
  }
}

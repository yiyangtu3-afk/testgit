package com.campuslink.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.campuslink.CampusLinkApplication;
import com.campuslink.entity.DemoEntities.AttachmentEntity;
import com.campuslink.entity.DemoEntities.MessageEntity;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    classes = CampusLinkApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = "spring.sql.init.mode=never")
@Transactional
@Rollback
class ChatRepositoryIntegrationTest {

  @Autowired
  private ChatRepository chatRepository;

  @Test
  void savesAndPaginatesMessageWithAttachmentsWithoutPersistingTestData() {
    MessageEntity saved = chatRepository.saveMessage(
        "u-2001",
        "u-1001",
        "事务集成测试消息",
        List.of(new AttachmentEntity("att-integration-1", "proof.txt", 12, "text/plain", "document")));

    List<MessageEntity> page = chatRepository.findMessagePage("u-2001", "u-1001", null, 1);

    assertThat(page).hasSize(1);
    assertThat(page.getFirst().id()).isEqualTo(saved.id());
    assertThat(page.getFirst().text()).isEqualTo("事务集成测试消息");
    assertThat(page.getFirst().attachments()).extracting(AttachmentEntity::name).containsExactly("proof.txt");
  }

  @Test
  void unreadCountsIgnoreFriendMessagesAddressedToAnotherUser() {
    MessageEntity thirdPartyMessage = chatRepository.saveMessage(
        "u-1001",
        "u-2002",
        "只发给林一的消息",
        List.of());
    chatRepository.markConversationRead(
        "u-2001", "u-2002", thirdPartyMessage.id() - 1);

    Map<String, Integer> unreadCounts = chatRepository.unreadCounts("u-2001");

    assertThat(unreadCounts).doesNotContainKey("u-2002");
  }

  @Test
  void unreadCountsTrackReceivedMessagesAfterTheReadCursor() {
    MessageEntity alreadyRead = chatRepository.saveMessage(
        "u-2001",
        "u-2002",
        "陈老师已读的消息",
        List.of());
    chatRepository.markConversationRead("u-2001", "u-2002", alreadyRead.id());
    MessageEntity unread = chatRepository.saveMessage(
        "u-2001",
        "u-2002",
        "陈老师尚未读的消息",
        List.of());

    assertThat(chatRepository.unreadCounts("u-2001"))
        .containsEntry("u-2002", 1);

    chatRepository.markConversationRead("u-2001", "u-2002", unread.id());
    assertThat(chatRepository.unreadCounts("u-2001"))
        .doesNotContainKey("u-2002");
  }
}

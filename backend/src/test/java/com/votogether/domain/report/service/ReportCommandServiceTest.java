package com.votogether.domain.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.votogether.domain.member.entity.Member;
import com.votogether.domain.post.dto.response.post.PostResponse;
import com.votogether.domain.post.entity.Post;
import com.votogether.domain.post.entity.comment.Comment;
import com.votogether.domain.post.entity.vo.PostClosingType;
import com.votogether.domain.post.entity.vo.PostSortType;
import com.votogether.domain.post.service.PostCommentService;
import com.votogether.domain.post.service.PostGuestService;
import com.votogether.domain.report.dto.request.ReportRequest;
import com.votogether.domain.report.entity.vo.ReportType;
import com.votogether.global.exception.BadRequestException;
import com.votogether.global.exception.NotFoundException;
import com.votogether.test.ServiceTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ReportCommandServiceTest extends ServiceTest {

    @Autowired
    ReportCommandService reportCommandService;

    @Autowired
    PostGuestService postGuestService;

    @Autowired
    PostCommentService postCommentService;

    @Nested
    @DisplayName("게시글 신고기능은")
    class ReportPost {

        @Test
        @DisplayName("정상적으로 동작한다.")
        void reportPost() {
            // given
            Member reporter = memberTestPersister.builder().save();
            Post post = postTestPersister.postBuilder().save();
            ReportRequest request = new ReportRequest(ReportType.POST, post.getId(), "불건전한 게시글");

            // when, then
            assertDoesNotThrow(() -> reportCommandService.report(reporter, request));
        }

        @Test
        @DisplayName("없는 투표글을 신고하는 경우 예외가 발생한다.")
        void reportNonExistPostThrowsException() {
            // given
            Member writer = memberTestPersister.builder().save();
            ReportRequest request = new ReportRequest(ReportType.POST, -1L, "불건전한 게시글");

            // when, then
            assertThatThrownBy(() -> reportCommandService.report(writer, request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("게시글이 존재하지 않습니다.");
        }

        @Test
        @DisplayName("자신의 투표글을 신고하는 경우 예외가 발생한다.")
        void reportOwnPostThrowsException() {
            // given
            Member writer = memberTestPersister.builder().save();
            Post post = postTestPersister.postBuilder().writer(writer).save();
            ReportRequest request = new ReportRequest(ReportType.POST, post.getId(), "불건전한 게시글");

            // when, then
            assertThatThrownBy(() -> reportCommandService.report(writer, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("본인 게시글은 신고할 수 없습니다.");
        }

        @Test
        @DisplayName("블라인드 처리된 투표글을 신고하는 경우 예외가 발생한다.")
        void reportHiddenPost() {
            // given
            Member reporter = memberTestPersister.builder().save();
            Post post = postTestPersister.postBuilder().save();
            post.blind();
            ReportRequest request = new ReportRequest(ReportType.POST, post.getId(), "불건전한 게시글");

            // when, then
            assertThatThrownBy(() -> reportCommandService.report(reporter, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("신고에 의해 숨겨진 게시글은 접근할 수 없습니다.");
        }

        @Test
        @DisplayName("하나의 회원이 투표글을 중복하여 신고하면 예외를 던진다.")
        void reportDuplicated() {
            // given
            Member reporter = memberTestPersister.builder().save();
            Post post = postTestPersister.postBuilder().save();
            ReportRequest request = new ReportRequest(ReportType.POST, post.getId(), "불건전한 게시글");

            // when
            reportCommandService.report(reporter, request);

            // then
            assertThatThrownBy(() -> reportCommandService.report(reporter, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("하나의 글에 대해서 중복하여 신고할 수 없습니다.");
        }

        @Test
        @DisplayName("투표글 신고가 5회가 되면 블라인드 처리가 된다.")
        void reportAndBlind() {
            // given
            Member reporter1 = memberTestPersister.builder().save();
            Member reporter2 = memberTestPersister.builder().save();
            Member reporter3 = memberTestPersister.builder().save();
            Member reporter4 = memberTestPersister.builder().save();
            Member reporter5 = memberTestPersister.builder().save();
            Post post = postTestPersister.postBuilder().save();
            ReportRequest request = new ReportRequest(ReportType.POST, post.getId(), "불건전한 게시글");

            // when
            reportCommandService.report(reporter1, request);
            reportCommandService.report(reporter2, request);
            reportCommandService.report(reporter3, request);
            reportCommandService.report(reporter4, request);
            reportCommandService.report(reporter5, request);

            // then
            final List<PostResponse> responses = postGuestService.getPosts(
                    0,
                    PostClosingType.ALL,
                    PostSortType.HOT,
                    null
            );
            assertAll(
                    () -> assertThat(post.isHidden()).isTrue(),
                    () -> assertThat(responses).isEmpty()
            );
        }

    }

    @Nested
    @DisplayName("댓글 신고기능은")
    class ReportComment {

        @Test
        @DisplayName("정상적으로 동작한다.")
        void reportComment() {
            // given
            Member reporter = memberTestPersister.builder().save();
            Post post = postTestPersister.postBuilder().save();
            Comment comment = commentTestPersister.builder().post(post).save();
            ReportRequest request = new ReportRequest(ReportType.COMMENT, comment.getId(), "불건전한 게시글");

            // when, then
            assertDoesNotThrow(() -> reportCommandService.report(reporter, request));
        }

        @Test
        @DisplayName("없는 댓글을 신고하는 경우 예외가 발생한다.")
        void reportNonExistCommentThrowsException() {
            // given
            Member writer = memberTestPersister.builder().save();
            ReportRequest request = new ReportRequest(ReportType.COMMENT, -1L, "불건전한 댓글");

            // when, then
            assertThatThrownBy(() -> reportCommandService.report(writer, request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("댓글이 존재하지 않습니다.");
        }

        @Test
        @DisplayName("자신의 댓글을 신고하는 경우 예외가 발생한다.")
        void reportOwnCommentThrowsException() {
            // given
            Member writer = memberTestPersister.builder().save();
            Post post = postTestPersister.postBuilder().save();
            Comment comment = commentTestPersister.builder().post(post).writer(writer).save();
            ReportRequest request = new ReportRequest(ReportType.COMMENT, comment.getId(), "불건전한 댓글");

            // when, then
            assertThatThrownBy(() -> reportCommandService.report(writer, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("본인 댓글은 신고할 수 없습니다.");
        }

        @Test
        @DisplayName("블라인드 처리된 댓글을 신고하는 경우 예외가 발생한다.")
        void reportHiddenComment() {
            // given
            Member reporter = memberTestPersister.builder().save();
            Post post = postTestPersister.postBuilder().save();
            Comment comment = commentTestPersister.builder().post(post).save();
            comment.blind();
            ReportRequest request = new ReportRequest(ReportType.COMMENT, comment.getId(), "불건전한 댓글");

            // when, then
            comment.blind();
            assertThatThrownBy(() -> reportCommandService.report(reporter, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("신고에 의해 숨겨진 댓글은 접근할 수 없습니다.");
        }

        @Test
        @DisplayName("하나의 회원이 댓글을 중복하여 신고하면 예외를 던진다.")
        void reportDuplicated() {
            // given
            Member reporter = memberTestPersister.builder().save();
            Post post = postTestPersister.postBuilder().save();
            Comment comment = commentTestPersister.builder().post(post).save();
            ReportRequest request = new ReportRequest(ReportType.COMMENT, comment.getId(), "불건전한 댓글");

            // when
            reportCommandService.report(reporter, request);

            // then
            assertThatThrownBy(() -> reportCommandService.report(reporter, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("하나의 댓글에 대해서 중복하여 신고할 수 없습니다.");
        }

        @Test
        @DisplayName("댓글 신고가 5회가 되면 블라인드 처리가 된다.")
        void reportAndBlind() {
            // given
            Member reporter1 = memberTestPersister.builder().save();
            Member reporter2 = memberTestPersister.builder().save();
            Member reporter3 = memberTestPersister.builder().save();
            Member reporter4 = memberTestPersister.builder().save();
            Member reporter5 = memberTestPersister.builder().save();
            Post post = postTestPersister.postBuilder().save();
            Comment comment = commentTestPersister.builder().post(post).save();
            ReportRequest request = new ReportRequest(ReportType.COMMENT, comment.getId(), "불건전한 댓글");

            // when
            reportCommandService.report(reporter1, request);
            reportCommandService.report(reporter2, request);
            reportCommandService.report(reporter3, request);
            reportCommandService.report(reporter4, request);
            reportCommandService.report(reporter5, request);

            // then
            assertAll(
                    () -> assertThat(comment.isHidden()).isTrue(),
                    () -> assertThat(postCommentService.getComments(post.getId())).isEmpty()
            );
        }

    }

    @Nested
    @DisplayName("닉네임 신고기능은")
    class ReportNickname {

        @Test
        @DisplayName("정상적으로 동작한다.")
        void reportNickname() {
            // given
            Member reporter = memberTestPersister.builder().save();
            Member reported = memberTestPersister.builder().save();
            ReportRequest request = new ReportRequest(ReportType.NICKNAME, reported.getId(), "불건전한 닉네임");

            // when, then
            assertDoesNotThrow(() -> reportCommandService.report(reporter, request));
        }

        @Test
        @DisplayName("자신의 닉네임을 신고하는 경우 예외가 발생한다.")
        void reportOwnNicknameThrowsException() {
            // given
            Member reporter = memberTestPersister.builder().save();
            ReportRequest request = new ReportRequest(ReportType.NICKNAME, reporter.getId(), "불건전한 닉네임");

            // when, then
            assertThatThrownBy(() -> reportCommandService.report(reporter, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("자신의 닉네임은 신고할 수 없습니다.");
        }

        @Test
        @DisplayName("하나의 회원이 다른 회원의 닉네임을 중복하여 신고하면 예외를 던진다.")
        void reportDuplicated() {
            // given
            Member reporter = memberTestPersister.builder().save();
            Member reported = memberTestPersister.builder().save();
            ReportRequest request = new ReportRequest(ReportType.NICKNAME, reported.getId(), "불건전한 닉네임");

            // when
            reportCommandService.report(reporter, request);

            // then
            assertThatThrownBy(() -> reportCommandService.report(reporter, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("하나의 닉네임에 대해서 중복하여 신고할 수 없습니다.");
        }

        @Test
        @DisplayName("닉네임 신고가 3회가 되면 닉네임이 자동변경처리가 된다.")
        void reportAndBlind() {
            // given
            Member reporter1 = memberTestPersister.builder().save();
            Member reporter2 = memberTestPersister.builder().save();
            Member reporter3 = memberTestPersister.builder().save();
            Member reported = memberTestPersister.builder().save();
            ReportRequest request = new ReportRequest(ReportType.NICKNAME, reported.getId(), "불건전한 닉네임");

            // when
            reportCommandService.report(reporter1, request);
            reportCommandService.report(reporter2, request);
            reportCommandService.report(reporter3, request);

            // then
            assertThat(reported.getNickname()).contains("Pause1");
        }
    }

}

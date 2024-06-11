package roomescape.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.exception.NotFoundException;
import roomescape.model.Member;
import roomescape.model.Payment;
import roomescape.model.Reservation;
import roomescape.model.ReservationTime;
import roomescape.model.Theme;
import roomescape.model.Waiting;
import roomescape.model.WaitingWithRank;
import roomescape.repository.PaymentRepository;
import roomescape.repository.ReservationRepository;
import roomescape.repository.WaitingRepository;
import roomescape.response.MemberReservationResponse;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReservationWaitingService {

    private final PaymentRepository paymentRepository;
    private final WaitingRepository waitingRepository;
    private final ReservationRepository reservationRepository;

    public ReservationWaitingService(final PaymentRepository paymentRepository,
                                     final WaitingRepository waitingRepository,
                                     final ReservationRepository reservationRepository) {
        this.paymentRepository = paymentRepository;
        this.waitingRepository = waitingRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional(readOnly = true)
    public List<MemberReservationResponse> getAllMemberReservationsAndWaiting(Member member) {
        List<Payment> payments = paymentRepository.findByMember(member);
        List<WaitingWithRank> waitingWithRanks = waitingRepository.findWaitingWithRankByMemberId(member.getId());

        List<MemberReservationResponse> allMemberReservations = new java.util.ArrayList<>(payments.stream()
                .map(MemberReservationResponse::new)
                .toList());
        List<MemberReservationResponse> waiting = waitingWithRanks.stream()
                .map(MemberReservationResponse::new)
                .toList();

        allMemberReservations.addAll(waiting);
        return allMemberReservations;
    }

    @Transactional
    public void deleteReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id).orElseThrow(() ->
                new NotFoundException("해당 id:[%s] 값으로 예약된 내역이 존재하지 않습니다.".formatted(id)));
        reservationRepository.deleteById(id);

        Theme theme = reservation.getTheme();
        LocalDate date = reservation.getDate();
        ReservationTime time = reservation.getTime();
        if (waitingRepository.existsWaitingByThemeAndDateAndTime(theme, date, time)) {
            convertWaitingToReservation(theme, date, time);
        }
    }

    private void convertWaitingToReservation(Theme theme, LocalDate date, ReservationTime time) {
        Waiting waiting = waitingRepository.findFirstByThemeAndDateAndTime(theme, date, time).orElseThrow(() ->
                new NotFoundException("해당 테마:[%s], 날짜:[%s], 시간:[%s] 값으로 예약된 예약 대기 내역이 존재하지 않습니다.".formatted(theme.getName(), date, time.getStartAt())));

        reservationRepository.save(new Reservation(date, time, theme, waiting.getMember()));

        waitingRepository.deleteById(waiting.getId());
    }
}

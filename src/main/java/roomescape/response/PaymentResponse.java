package roomescape.response;

public class PaymentResponse {
    private final Long totalAmount;
    private final String paymentKey;

    public PaymentResponse(final Long totalAmount, final String paymentKey) {
        this.totalAmount = totalAmount;
        this.paymentKey = paymentKey;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public String getPaymentKey() {
        return paymentKey;
    }
}

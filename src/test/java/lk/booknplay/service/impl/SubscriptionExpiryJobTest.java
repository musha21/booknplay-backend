package lk.booknplay.service.impl;

import lk.booknplay.service.OwnerSubscriptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionExpiryJobTest {

    @Mock
    private OwnerSubscriptionService ownerSubscriptionService;

    @Test
    void expireSubscriptionsAndArchiveVenues_DelegatesToService() {
        when(ownerSubscriptionService.expireDueSubscriptionsAndArchiveVenues()).thenReturn(2);
        SubscriptionExpiryJob job = new SubscriptionExpiryJob(ownerSubscriptionService);

        job.expireSubscriptionsAndArchiveVenues();

        verify(ownerSubscriptionService).expireDueSubscriptionsAndArchiveVenues();
    }
}

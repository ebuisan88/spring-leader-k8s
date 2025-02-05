package com.example.demo;

import io.fabric8.kubernetes.api.model.coordination.v1.Lease;
import io.fabric8.kubernetes.api.model.coordination.v1.LeaseBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.time.ZoneOffset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class JobCoordinatorService {

    private static final String LEASE_NAME = "spring-leader-election";
    private static final String NAMESPACE = "default";
    private static final Integer LEADER_GRACE_PERIOD = 15;

    private static final Logger logger = LoggerFactory.getLogger(JobCoordinatorService.class);

    @Autowired
    private KubernetesClient kubernetesClient;

    private final String holderIdentity = UUID.randomUUID().toString();

    /**
     * This method checks the current lease status in the Kubernetes cluster.
     * If the lease is available or expired, it tries to acquire the lease.
     * If the lease is held by another instance, it logs the holder identity.
     * The method is scheduled to run at a fixed delay of 5000 milliseconds.
     */
    @Scheduled(fixedDelay = 5000)
    public void checkLease() {
        try {
            Lease lease = kubernetesClient.resources(Lease.class)
                    .inNamespace(NAMESPACE)
                    .withName(LEASE_NAME)
                    .get();
            if (isLeaseAvailable(lease)) {
                logger.info("[" + holderIdentity + "] " +
                        "lease: " + lease.getSpec().toString());
                acquireLease(lease);
                logger.info("[" + holderIdentity + "] " + "lease acquired");
            } else {
                logger.info("[" + holderIdentity + "] " + "lease held by: " + lease.getSpec().getHolderIdentity());
            }
        } catch (KubernetesClientException e) {
            logger.error("[" + holderIdentity + "] " + "error accessing Kubernetes API: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("[" + holderIdentity + "] " + "unexpected error: " + e.getMessage(), e);
        }
    }

    public boolean isLeader() {
        Lease lease = kubernetesClient.resources(Lease.class)
                .inNamespace(NAMESPACE)
                .withName(LEASE_NAME)
                .get();

        if (lease == null) {
            return false;
        }

        if (lease == null || lease.getSpec() == null) {
            return false;
        }
        return holderIdentity.equals(lease.getSpec().getHolderIdentity());
    }

    public boolean isLeader(Lease lease) {
        return lease != null && lease.getSpec() != null && holderIdentity.equals(lease.getSpec().getHolderIdentity());
    }

    public void releaseLease() {
        Lease lease = kubernetesClient.resources(Lease.class)
                .inNamespace(NAMESPACE)
                .withName(LEASE_NAME)
                .get();

        if (lease == null) {
            return;
        }

        if (isLeader(lease)) {
            logger.info("[" + holderIdentity + "] " +
                    "releasing lease: " + lease.getSpec().toString());
            Lease updatedLease = new LeaseBuilder(
                    lease)
                    .withNewSpec()
                    .withHolderIdentity(null)
                    .withRenewTime(OffsetDateTime.now().atZoneSameInstant(ZoneOffset.UTC))
                    .withLeaseDurationSeconds(lease.getSpec().getLeaseDurationSeconds())
                    .endSpec()
                    .build();

            kubernetesClient.resources(Lease.class)
                    .inNamespace(NAMESPACE)
                    .withName(LEASE_NAME)
                    .createOrReplace(updatedLease);
            logger.info("[" + holderIdentity + "] " +
                    "released lease: " + lease.getSpec().toString());
        }
    }

    private boolean isLeaseAvailable(Lease lease) {
        if (isLeaseNullOrInvalid(lease)) {
            return true;
        }
        if (lease.getSpec().getHolderIdentity() == null || lease.getSpec().getHolderIdentity().isEmpty()) {
            return true;
        }
        if (isLeader(lease)) {
            return isLeaseExpiredWithGracePeriod(lease);
        }
        return isLeaseExpired(lease);
    }

    private boolean isLeaseNullOrInvalid(Lease lease) {
        return lease == null || lease.getSpec() == null;
    }

    private boolean isLeaseExpiredWithGracePeriod(Lease lease) {
        Integer minusSeconds = lease.getSpec().getLeaseDurationSeconds() - LEADER_GRACE_PERIOD;
        return lease.getSpec().getRenewTime() == null ||
                lease.getSpec().getHolderIdentity() == null ||
                lease.getSpec().getHolderIdentity().isEmpty() ||
                lease.getSpec().getRenewTime()
                        .isBefore(OffsetDateTime.now().minusSeconds(minusSeconds).atZoneSameInstant(ZoneOffset.UTC));
    }

    private boolean isLeaseExpired(Lease lease) {
        return lease.getSpec().getRenewTime() == null ||
                lease.getSpec().getHolderIdentity() == null ||
                lease.getSpec().getHolderIdentity().isEmpty() ||
                lease.getSpec().getRenewTime()
                        .isBefore(OffsetDateTime.now().minusSeconds(lease.getSpec().getLeaseDurationSeconds())
                                .atZoneSameInstant(ZoneOffset.UTC));
    }

    private void acquireLease(Lease existingLease) {
        Lease updatedLease = new LeaseBuilder(existingLease)
                .withNewSpec()
                .withHolderIdentity(holderIdentity)
                .withRenewTime(OffsetDateTime.now().atZoneSameInstant(ZoneOffset.UTC))
                .withLeaseDurationSeconds(existingLease.getSpec().getLeaseDurationSeconds())
                .endSpec()
                .build();
        try {
            kubernetesClient.resources(Lease.class)
                    .inNamespace(NAMESPACE)
                    .withName(LEASE_NAME)
                    .patch(updatedLease);
        } catch (KubernetesClientException e) {
            if (e.getCode() == 409) {
                logger.warn("[" + holderIdentity + "] " + "lease conflict: " + e.getMessage());
            } else {
                throw e;
            }
        }
    }

}
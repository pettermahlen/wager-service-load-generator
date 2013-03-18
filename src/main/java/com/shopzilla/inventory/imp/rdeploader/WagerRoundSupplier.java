package com.shopzilla.inventory.imp.rdeploader;

import com.google.common.base.Supplier;
import com.williamsinteractive.casino.wager.api.OutcomeRequest;
import com.williamsinteractive.casino.wager.api.WagerRequest;

import java.security.SecureRandom;
import java.util.Random;

import static com.shopzilla.inventory.imp.rdeploader.WagerRoundState.NEW;

/**
 * TODO: document!
 *
 * @author Petter Måhlén
 */
public class WagerRoundSupplier
    implements Supplier<Object> {

    private static final Random RANDOM = new SecureRandom();

    private final ThreadLocal<WagerRound> wagerRoundStateThreadLocal = new ThreadLocal<WagerRound>() {
        @Override
        protected WagerRound initialValue() {
            return new WagerRound(nextId(), NEW);
        }
    };

    @Override
    public Object get() {
        WagerRound currentRound = wagerRoundStateThreadLocal.get();

        switch (currentRound.state) {
            case NEW:
                switchTo(WagerRoundState.PLACED);
                return placeRequest(currentRound.wagerRoundId);
            case PLACED:
                if (placeAnotherBet()) {
                    return placeRequest(currentRound.wagerRoundId);
                }
                else {
                    long currentWagerRoundId = currentRound.wagerRoundId;
                    switchTo(NEW);
                    return outcomeRequest(currentWagerRoundId);
                }
            default:
                throw new IllegalStateException();
        }
    }

    private boolean placeAnotherBet() {
        return false; // TODO: may want to randomly generate additional bets, maybe
    }

    private void switchTo(WagerRoundState newState) {
        long nextWagerRoundId = wagerRoundStateThreadLocal.get().wagerRoundId;

        if (newState == NEW) {
            nextWagerRoundId = nextId();
        }

        wagerRoundStateThreadLocal.set(new WagerRound(nextWagerRoundId, newState));
    }

    private WagerRequest placeRequest(long wagerRoundId) {
        return new WagerRequest(wagerRoundId, nextId(), nextAmount(), nextId(), nextId());
    }

    private OutcomeRequest outcomeRequest(long currentWagerRoundId) {
        return new OutcomeRequest(currentWagerRoundId, nextId(), nextAmount());
    }

    private static long nextId() {
        return Math.abs(RANDOM.nextLong());
    }

    private static int nextAmount() {
        return Math.abs(RANDOM.nextInt());
    }

    private static class WagerRound {
        private final long wagerRoundId;
        private final WagerRoundState state;

        private WagerRound(long wagerRoundId, WagerRoundState state) {
            this.wagerRoundId = wagerRoundId;
            this.state = state;
        }
    }


}

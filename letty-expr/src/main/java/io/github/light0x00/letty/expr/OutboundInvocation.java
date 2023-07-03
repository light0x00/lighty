package io.github.light0x00.letty.expr;

interface OutboundInvocation {

    void invoke(Object arg,ListenableFutureTask<Void> future);

}

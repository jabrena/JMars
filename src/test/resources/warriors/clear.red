; Clear - sequentially writes DAT bombs through core.
; The pointer lives in the final DAT instruction.
mov.i $2, >3
jmp.b $-1, $0
dat.f #0, #0
dat.f #0, #0

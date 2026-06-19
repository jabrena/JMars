; Paper - tiny process-splitting mover.
; It is not a full competitive paper, but it creates busy visual traces.
spl.b $1, $0
mov.i $0, $1
spl.b $-1, $0
jmp.b $-2, $0

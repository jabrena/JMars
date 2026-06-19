; Runner - a tiny process-splitting mover for visual smoke tests.
; This is intentionally simple and parser-friendly.
spl.b $1, $0
mov.i $0, $1
jmp.b $-1, $0

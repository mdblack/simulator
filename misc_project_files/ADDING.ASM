        assume cs:code, ds:code
code    segment
start:
	mov	ax,1234h
        mov     ax,code
        mov     ds,ax

; print prompt
        mov     ah,9
        mov     dx,offset entermessage
        int     21h
; read a character
	mov	ah,1
	int	21h	;ask dos to do read
;char is in al
	sub	al,48	;al=al-'0'
	mov	bl,al	;save in bl
; do it again
; print prompt
        mov     ah,9
        mov     dx,offset entermessage
        int     21h
; read a character
	mov	ah,1
	int	21h	;ask dos to do read
;char is in al
	sub	al,48	;al=al-'0'

; lets do the add
	add	bl,al	;bl=bl+al

;print your answer is
        mov     ah,9
        mov     dx,offset answermessage
        int     21h

; add the 48 again
	add	bl,48	;bl+='0'

;print out char
	mov	ah,2
	mov	dl,bl	;put it in the printing place
	int	21h
;quit prog
        mov     ah,0
        int     21h

entermessage	db	13,10,'Enter a number: $'
answermessage	db	13,10,'The sum is $'

code    ends
        end     start

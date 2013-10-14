	assume	cs:code,ds:data		;implicitly use "code" for jumps, "data" for moves
data	segment
first	db	'Enter a first number: ','$'
second db	'Enter a second number: ','$'
data	ends

code	segment
start:
	cli
	mov	ax,1234h

	mov	ax,data
	mov	ds,ax

	mov	ah,9
	mov	dx,offset first
	int	21h			;prompt for first number


	mov	ah,1
	int	21h			; al is now the character

	sub	al,30h		; al is now the number

	mov	bl,al			; bl is my number

	mov	ah,9
	mov	dx,offset second
	int	21h			;prompt for second number

	mov	ah,1
	int	21h			; al is now the character

	sub	al,30h		; al is now the number


	call	multiply

	mov	ah,2
	mov	dl,cl			; dl is char to print
	add	dl,30h		; dl is now a char
	int	21h


	mov	ah,4ch
	int	21h

multiply:
; ready to multiply al and bl
	mov	cl,0		;s=0
	mov	dl,0		;i=0

multiplyloop:
	cmp	dl,al		; i-a --> flags
	jge	exitloop	; is i>=0?
				;check if i>=a
	add	cl,bl		;s=s+b
	add	dl,1		;i++
	jmp	multiplyloop	;goto loop

exitloop:
	ret

code	ends
end	start

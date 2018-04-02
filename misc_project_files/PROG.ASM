	assume cs:code, ds:data, es:nothing
code 	segment
start:
	mov	ax,1234h

;ds:dx to point userinput
	mov	ax,data
	mov	ds,ax
	mov	dx, offset userinput
	mov	ah,0ah
	int	21h

; get memory[si+1] into reg
	mov	si, offset userinput
	add	si,1
	mov	al,[si]	;assumes that si means ds

	mov	ah,2
	mov	dl,al	;print out size of input
	add	dl,'0'
	int	21h

	mov	cl,'J'
	mov	si,0	;offset 0
	mov	ax,0b800h
	mov	ds,ax	;es is vram
	mov	[si],cl
	mov	cl,2
	add	si,1
	mov	[si],cl

here:	jmp	here

	mov 	ah,0
	int 	21h
code	ends

data	segment
userinput	db	15,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
data	ends

	end 	start
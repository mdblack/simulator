	assume cs:code, ss:stackseg, ds:data, es:nothing
stackseg	segment
thestack	db	100 dup(?)
stackplace	db	0	
stackseg	ends

code 	segment
start:
	mov	ax,1234h

	mov	ax,stackseg
	mov	ss,ax
	mov	sp,offset stackplace

;ds:dx to point userinput
	mov	ax,data
	mov	ds,ax

;set up isr for interrupt 42
	push	ds
	mov	ax,0
	mov	ds,ax	;ds is IVT
	mov	si,108h	;IVT entry for int 42
	mov	ax,offset exittodos
	mov	[si],ax
	add	si,2
	mov	ax,code
	mov	[si],ax
	pop	ds

	call	repaint

keyloop:	in	al,64h	;read - is there a key?
	and	al,1
	jz	keyloop
	;there was a keystroke
	in	al,60h	;get the key
	cmp	al,4bh	;left?
	jz	keyleft
	cmp	al,4dh	;right?
	jz	keyright
	cmp	al,39h	;space?
	jz	keyspace
	call	clearkeybuffer
	jmp	keyloop

keyleft:
	call	clearkeybuffer
	mov	al,Jx
	sub	al,1
	mov	Jx,al
	call	repaint
	jmp	keyloop
keyright:
	call	clearkeybuffer
	mov	al,Jx
	add	al,1
	mov	Jx,al
	call	repaint
	jmp	keyloop
keyspace:
	call	clearkeybuffer
	int	42h
clearkeybuffer:
	in	al,60h	;read from buffer
	in	al,64h	;read from command
	and	al,1	
	jnz	clearkeybuffer
	ret

here:	sti
	jmp	here

;interrupt service routine
exittodos:
	mov 	ah,0
	int 	21h

repaint:	mov	cl,'J'
	mov	si,780h	;offset of middle of screen
	push	si

	mov	dl,Jx
	shl	dl,1
	pop	si
	mov	dh,0
	add	si,dx

	push	ds
	mov	ax,0b800h
	mov	ds,ax	;es is vram
	mov	[si],cl
	mov	cl,2
	add	si,1
	mov	[si],cl
	pop	ds
	ret

code	ends

data	segment
Jx	db	40	;initial x

userinput	db	15,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
data	ends

	end 	start
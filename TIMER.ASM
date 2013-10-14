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

	push	ds	;save my ds
	mov	ax,0
	mov	ds,ax	;segment 0

	mov	si,32	;32 = int 8 * 4
	mov	ax,offset myisr
	mov	[si],ax
	add	si,2
	mov	ax,code
	mov	[si],ax

	pop	ds
	cli
; set up timer
	mov	al,36h	;set up timer
	out	43h,al
	mov	al,0ffh
	out	40h,al
	mov	al,0ffh
	out	40h,al
	;hopefully, timer is set to go off in 10 cycles

	sti
here:
	jmp	here

exittodos:
	mov 	ah,0
	int 	21h


myisr:
	mov	ah,9
	mov	dx,offset isrmsg
	int	21h
	mov	al,20h
	out	20h,al	;reset timer and go again
	sti
	iret

code	ends

data	segment
isrmsg	db	"Hi from the ISR! $"
data	ends

	end 	start
	assume cs:code, ss:code, ds:code, es:nothing
code 	segment
start:
	mov	ax,1234h

	mov	ax,code
	mov	ds,ax

	;set up a timer interrupt service routine
	push	ds	;save my ds
	; set ds to interrupt table
	mov	ax,0
	mov	ds,ax	;segment 0

	; redirect interrupt 8
	; put in the entry: myisr, code segment
	mov	si,32	;32 = int 8 * 4
	mov	ax,offset myisr
	mov	[si],ax
	add	si,2
	mov	ax,code
	mov	[si],ax

	pop	ds

	; stop interrupts from happening
	cli

	; set up timer
	mov	al,36h
	out	43h,al

	mov	al,0ffh
	out	40h,al
	mov	al,0ffh
	out	40h,al
	;hopefully, timer is set to go off in 64k cycles

	; now let the interrupts go
	sti

	;hang up program here
here:
	jmp	here

myisr:
	mov	ah,9
	mov	dx,offset isrmsg
	int	21h
	mov	al,20h
	out	20h,al	;reset timer and go again
	sti
	iret

isrmsg	db	"Interrupt $"

code	ends

	end 	start

	assume cs:code, ds:dataseg
dataseg	segment
hello	db	'Hello World',13,10,'$'
dataseg	ends
code	segment
start:
	mov	ax,1234h
	mov	ax,dataseg
	mov	ds,ax
	mov	ah,9
	mov	dx,offset hello
	int	21h
	mov	ah,4ch
	int	21h
code	ends
end	start

.global main
.global _main
.text
main:
call _main
# move the return value into the first argument for the syscall
movq %rax, %rdi
# move the exit syscall number into rax
movq $0x3C, %rax
syscall
#own-code
_main:
mov $42, %ebx
mov $3, %ecx
sub %ecx, %ebx

mov %ebx, %eax
cdq
idiv %ecx
mov %eax, %esi

mov %ebx, %eax
cdq
idiv %ecx
mov %edx, %ebx

imul %ecx, %esi

add %ebx, %esi
mov %esi, %ebx

add %ecx, %ebx

mov %ebx, %eax
ret


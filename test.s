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
mov $0, %ebx
mov $-2147483648, %ecx
mov %ebx, %esi
sub %ecx, %esi
mov %esi, %eax
cdq
idiv %esi
mov %eax, %edi
mov %ebx, %r8d
sub %edi, %r8d
mov %esi, %eax
cdq
idiv %r8d
mov %eax, %r9d
mov %r9d, %eax
ret


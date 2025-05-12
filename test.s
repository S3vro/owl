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
mov $1, %ebx
mov %ebx, %ecx
add %ebx, %ecx
mov %ecx, %esi
add %ebx, %esi
mov %esi, %edi
add %ebx, %edi
mov %edi, %r8d
add %ebx, %r8d
mov %r8d, %r9d
add %ebx, %r9d
mov %r9d, %r10d
add %ebx, %r10d
mov %r10d, %r11d
add %ebx, %r11d
mov %r11d, %r12d
add %ebx, %r12d
mov %r12d, %eax
ret


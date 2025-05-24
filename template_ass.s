.intel_syntax noprefix 

.global main
.global _main
.text
main:
call _main
# move the return value into the first argument for the syscall
movq rdi, rax 
# move the exit syscall number into rax
movq rax, 0x3C
syscall
#own-code
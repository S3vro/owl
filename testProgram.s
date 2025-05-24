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
_main:
mov ebx, 98mov ecx, 57imul ebx, ecx
mov esi, 46mov edi, 93mov eax, esi
cdq
idiv edi
mov r8d, eax
sub ebx, r8d
mov r8d, 26add ebx, r8d
mov eax, ecx
cdq
idiv esi
mov ecx, edx
neg ecx
add ecx, ebx
add ecx, edi
add ecx, r8d
mov r9d, ebx
imul r9d, ecx
mov eax, ecx
cdq
idiv esi
mov esi, eax
mov eax, esi
cdq
idiv edi
mov esi, edx
imul esi, r8d
neg esi
add esi, ebx
mov eax, r9d
cdq
idiv esi
mov r9d, eax
mov eax, edi
cdq
idiv r8d
mov edi, edx
add edi, r9d
mov eax, edi
cdq
idiv r8d
mov edi, eax
sub ebx, ecx
add ebx, esi
sub ebx, edi
mov eax, ebx
ret

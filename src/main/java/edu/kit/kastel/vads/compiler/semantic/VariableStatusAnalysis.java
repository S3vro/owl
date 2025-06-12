package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.lexer.Operator;
import edu.kit.kastel.vads.compiler.parser.ast.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.BinaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.BitwiseNegateTree;
import edu.kit.kastel.vads.compiler.parser.ast.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.BoolLiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.BreakTree;
import edu.kit.kastel.vads.compiler.parser.ast.ContinueTree;
import edu.kit.kastel.vads.compiler.parser.ast.DeclarationTree;
import edu.kit.kastel.vads.compiler.parser.ast.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.IdentExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.IfTree;
import edu.kit.kastel.vads.compiler.parser.ast.IntLiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.LValueIdentTree;
import edu.kit.kastel.vads.compiler.parser.ast.LogicalNegateTree;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.NegateTree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.ast.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.StatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.TernaryTree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;
import edu.kit.kastel.vads.compiler.parser.ast.WhileTree;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class VariableStatusAnalysis implements Visitor<VariableStatusAnalysis.VariableStatus, VariableStatusAnalysis.VariableStatus> {

    private static List<Operator.OperatorType> needUsageCheck = List.of(
      Operator.OperatorType.ASSIGN_PLUS,
      Operator.OperatorType.ASSIGN_MINUS,
      Operator.OperatorType.ASSIGN_MUL,
      Operator.OperatorType.ASSIGN_DIV,
      Operator.OperatorType.ASSIGN_MOD,
      Operator.OperatorType.ASSIGN_AND,
      Operator.OperatorType.ASSIGN_XOR,
      Operator.OperatorType.ASSIGN_OR,
      Operator.OperatorType.ASSIGN_LSHIFT,
      Operator.OperatorType.ASSIGN_RSHIFT
    );

    @Override
    public VariableStatus visit(AssignmentTree assignmentTree, VariableStatus data) {
        VariableStatus status = assignmentTree.expression().accept(this, data);
        if (!(assignmentTree.lValue() instanceof LValueIdentTree)) {
            throw new SemanticException("Expected LValueIdentTree but got " + assignmentTree.lValue());
        }

        Name name = ((LValueIdentTree) assignmentTree.lValue()).name().name();

        if (needUsageCheck.contains(assignmentTree.operator().type())) {
            data.use(name);
        }

        return status.define(name);
    }

    @Override
    public VariableStatus visit(BinaryOperationTree binaryOperationTree, VariableStatus data) {
        VariableStatus status = binaryOperationTree.lhs().accept(this, data);
        status = binaryOperationTree.rhs().accept(this, status);
        return status;
    }

    @Override
    public VariableStatus visit(BlockTree blockTree, VariableStatus data) {
        VariableStatus status = data.newScope();
        for (StatementTree statementTree : blockTree.statements()) {
            status = statementTree.accept(this, status);
        }

        return status.exitScopeAndPropagate();
    }

    @Override
    public VariableStatus visit(DeclarationTree declarationTree, VariableStatus data) {
        VariableStatus status = data.declare(declarationTree.name().name());
        if (declarationTree.initializer() != null) {
            status = declarationTree.initializer().accept(this, status);
            status = status.define(declarationTree.name().name());
        }
        return status;
    }

    @Override
    public VariableStatus visit(FunctionTree functionTree, VariableStatus data) {
        return functionTree.body().accept(this, data);
    }

    @Override
    public VariableStatus visit(IdentExpressionTree identExpressionTree, VariableStatus data) {
        data.use(identExpressionTree.name().name());
        return data;
    }

    @Override
    public VariableStatus visit(IntLiteralTree literalTree, VariableStatus data) {
        return data;
    }

    @Override
    public VariableStatus visit(BoolLiteralTree literalTree, VariableStatus data) {
        return data;
    }

    @Override
    public VariableStatus visit(LValueIdentTree lValueIdentTree, VariableStatus data) {
        return data;
    }

    @Override
    public VariableStatus visit(NameTree nameTree, VariableStatus data) {
        return data;
    }

    @Override
    public VariableStatus visit(NegateTree negateTree, VariableStatus data) {
        return negateTree.expression().accept(this, data);
    }

    @Override
    public VariableStatus visit(ProgramTree programTree, VariableStatus data) {
        VariableStatus status = data;
        for (FunctionTree function : programTree.topLevelTrees()) {
            status = function.accept(this, status);
        }

        return status;
    }

    @Override
    public VariableStatus visit(ReturnTree returnTree, VariableStatus data) {
        VariableStatus status = returnTree.expression().accept(this, data);
        return status.allDefined();
    }

    @Override
    public VariableStatus visit(TypeTree typeTree, VariableStatus data) {
        return data;
    }

    @Override
    public VariableStatus visit(IfTree ifTree, VariableStatus data) {
        VariableStatus status = ifTree.e().accept(this, data).newScope();

        VariableStatus thenStatus = ifTree.then().accept(this, status).exitScopeAndPropagate();
        if(ifTree.orElse().isPresent()) {
            VariableStatus elseStatus = ifTree.orElse().get().accept(this, status).exitScopeAndPropagate();
            return elseStatus.calculateIf(thenStatus);
        }

        return status;
    }

    @Override
    public VariableStatus visit(WhileTree whileTree, VariableStatus data) {
        VariableStatus status = whileTree.condition().accept(this, data).newScope();
        status = whileTree.body().accept(this, status);
        return status.exitScope();
    }

    @Override
    public VariableStatus visit(LogicalNegateTree negateTree, VariableStatus data) {
        return negateTree.expression().accept(this, data);
    }

    @Override
    public VariableStatus visit(TernaryTree ternaryTree, VariableStatus data) {
        VariableStatus status = ternaryTree.condition().accept(this, data);
        status = ternaryTree.trueBranch().accept(this, status);
        status = ternaryTree.falseBranch().accept(this, status);
        return status;
    }

    @Override
    public VariableStatus visit(ForTree forTree, VariableStatus data) {
        VariableStatus status = data.newScope();
        status = forTree.definition().isPresent() ? forTree.definition().get().accept(this, status) : status;
        status = forTree.condition().accept(this, status);
        status = status.newScope().newScope();
        status = forTree.body().accept(this, status);
        status = status.exitScope();
        status = forTree.statement().isPresent() ? forTree.statement().get().accept(this, status) : status;
        return status.exitScope().exitScopeAndPropagate();
    }

    @Override
    public VariableStatus visit(BitwiseNegateTree bitwiseNegateTree, VariableStatus data) {
        return bitwiseNegateTree.expression().accept(this, data);
    }

    @Override
    public VariableStatus visit(BreakTree breakTree, VariableStatus data) {
        return data.allDefined();
    }

    @Override
    public VariableStatus visit(ContinueTree continueTree, VariableStatus data) {
        return data.allDefined();
    }

    record Scope(Set<Name> definitions, Set<Name> declarations) {
        public Scope copy() {
            return new Scope(new HashSet<>(Set.copyOf(definitions)), new HashSet<>(Set.copyOf(declarations)));
        }
    }

    static class VariableStatus {
        private List<Scope> scopes;

        public List<Scope> scopes() {
            return scopes;
        }

        public VariableStatus(List<Scope> scopes) {
            this.scopes = scopes;
        }

        public VariableStatus newScope() {
            List<Scope> copied = new ArrayList<>(List.copyOf(scopes));
            copied.add(new Scope(new HashSet<>(), new HashSet<>()));

            return new VariableStatus(copied);
        }

        public VariableStatus exitScopeAndPropagate() {
            if (scopes.size() <= 1) {
                return new VariableStatus(new ArrayList<>());
            }

            List<Scope> copiedScopes = new ArrayList<>(scopes);

            Scope current = copiedScopes.getLast();
            copiedScopes.removeLast();
            /*Add definitions that do not belong to the declarations here so that they can maybe declare other definitions*/
            Scope copied = copiedScopes.getLast().copy();
            copied.definitions().addAll(
              current.definitions().stream()
                .filter(c -> !current.declarations().contains(c))
                .toList()
            );
            copiedScopes.removeLast();
            copiedScopes.add(copied);
            return new VariableStatus(copiedScopes);
        }

        public VariableStatus exitScope() {
            if (scopes.size() <= 1) {
                return new VariableStatus(new ArrayList<>());
            }


            List<Scope> copied = new ArrayList<>(List.copyOf(scopes));
            copied.removeLast();

            return new VariableStatus(copied);
        }

        private boolean declared(Name name) {
           return scopes.stream().anyMatch(s -> s.declarations().contains(name));
        }

        private boolean defined(Name name) {
            return scopes.stream().anyMatch(s -> s.definitions().contains(name));
        }

        public VariableStatus define(Name name) {
            if (!declared(name)) {
                throw new SemanticException("Variable " + name + " was not declared in this scope, but you are trying to define it");
            }



            List<Scope> copiedScopes = new ArrayList<>(scopes);
            Scope copied = this.scopes.getLast().copy();
            copied.definitions().add(name);
            copiedScopes.removeLast();
            copiedScopes.addLast(copied);
            return new VariableStatus(copiedScopes);
        }

        public VariableStatus declare(Name name) {
            if (declared(name)) {
                throw new SemanticException("Variable " + name + " already declared!");
            }

            List<Scope> copiedScopes = new ArrayList<>(scopes);
            Scope copied = this.scopes.getLast().copy();
            copied.declarations().add(name);
            copiedScopes.removeLast();
            copiedScopes.addLast(copied);
            return new VariableStatus(copiedScopes);

        }

        public void use(Name name)  {
            if (!declared(name)) {
                throw new SemanticException("Variable " + name + " is not declared in current scope!");
            }

            if (!defined(name)) {
                throw new SemanticException("Variable " + name + " is not defined in current scope!");
            }
        }

        public VariableStatus allDefined() {
            if (scopes.isEmpty()) return new VariableStatus(new ArrayList<>());
            Set<Name> allDeclarations = new HashSet<>();
            scopes.forEach(s -> allDeclarations.addAll(s.declarations()));
            Set<Name> filtered = allDeclarations.stream().filter(name -> !defined(name)).collect(Collectors.toSet());


            List<Scope> copiedScopes = new ArrayList<>(scopes);
            Scope copied = this.scopes.getLast().copy();
            copied.definitions().addAll(filtered);
            copiedScopes.removeLast();
            copiedScopes.addLast(copied);
            return new VariableStatus(copiedScopes);
        }

        public VariableStatus calculateIf(VariableStatus afterThen) {
            Scope scopeA = this.scopes.isEmpty() ? new Scope(new HashSet<>(), new HashSet<>()) : this.scopes.getLast().copy();
            Scope scopeB = afterThen.scopes().isEmpty() ? new Scope(new HashSet<>(), new HashSet<>()) : afterThen.scopes().getLast().copy();

            List<Name> defs = scopeA.definitions().stream().filter(scopeB.definitions()::contains).toList();

            scopeA.definitions().clear();
            scopeA.definitions().addAll(defs);

            List<Scope> copiedScopes = new ArrayList<>(this.scopes);
            if (!this.scopes.isEmpty())
                copiedScopes.removeLast();
            copiedScopes.add(scopeA);

            return new VariableStatus(copiedScopes);
        }
    }
}

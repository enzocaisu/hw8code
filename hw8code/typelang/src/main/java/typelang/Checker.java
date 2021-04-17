package typelang;

import java.util.ArrayList;
import java.util.List;

import typelang.AST.*;
import typelang.Env.ExtendEnv;
import typelang.Env.GlobalEnv;
import typelang.Type.*;

public class Checker implements Visitor<Type,Env<Type>> {
	Printer.Formatter ts = new Printer.Formatter();
	Env<Type> initEnv = initialEnv(); //New for definelang

	private Env<Type> initialEnv() {
		GlobalEnv<Type> initEnv = new GlobalEnv<Type>();
		
		/* Type for procedure: (read <filename>). Following is same as (define read (lambda (file) (read file))) */
		List<Type> formalTypes = new ArrayList<Type>();
		formalTypes.add(new Type.StringT());
		initEnv.extend("read", new Type.FuncT(formalTypes, new Type.StringT()));

		/* Type for procedure: (require <filename>). Following is same as (define require (lambda (file) (eval (read file)))) */
		formalTypes = new ArrayList<Type>();
		formalTypes.add(new Type.StringT());
		initEnv.extend("eval", new Type.FuncT(formalTypes, new Type.UnitT()));
		
		/* Add type for new built-in procedures here */ 
		
		return initEnv;
	}
	
    Type check(Program p) {
		return (Type) p.accept(this, null);
	}

	public Type visit(Program p, Env<Type> env) {
		Env<Type> new_env = initEnv;

		for (DefineDecl d: p.decls()) {
			Type type = (Type)d.accept(this, new_env);

			if (type instanceof ErrorT) { return type; }

			Type dType = d.type();

			if (!type.typeEqual(dType)) {
				return new ErrorT("Expected " + dType + " found " + type + " in " + ts.visit(d, null));
			}

			new_env = new ExtendEnv<Type>(new_env, d.name(), dType);
		}
		return (Type) p.e().accept(this, new_env);
	}

	public Type visit(VarExp e, Env<Type> env) {
		try {
			return env.get(e.name());
		} catch(Exception ex) {
			return new ErrorT("Variable " + e.name() +
					" has not been declared in " + ts.visit(e, null));
		}
	}

	public Type visit(LetExp e, Env<Type> env) {
		List<String> names = e.names();
		List<Exp> value_exps = e.value_exps();
		List<Type> types = e.varTypes();
		List<Type> values = new ArrayList<Type>(value_exps.size());

		int i = 0;
		for (Exp exp : value_exps) {
			Type type = (Type)exp.accept(this, env);
			if (type instanceof ErrorT) { return type; }

			Type argType = types.get(i);
			if (!type.typeEqual(argType)) {
				return new ErrorT("The declared type of the " + i +
						" let variable and the actual type mismatch, expect " +
						argType.toString() + " found " + type +
						" in " + ts.visit(e, null));
			}

			values.add(type);
			i++;
		}

		Env<Type> new_env = env;
		for (int index = 0; index < names.size(); index++)
			new_env = new ExtendEnv<Type>(new_env, names.get(index),
					values.get(index));

		return (Type) e.body().accept(this, new_env);		
	}

	public Type visit(DefineDecl d, Env<Type> env) {
		String name = d.name();
		Type t =(Type) d._value_exp.accept(this, env);
		((GlobalEnv<Type>) initEnv).extend(name, t);
		return t;
	}

	public Type visit(LambdaExp e, Env<Type> env) {
		List<String> names = e.formals();
		List<Type> types = e.types();
		String message = "The number of formal parameters and the number of "
				+ "arguments in the function type do not match in ";
		if (types.size() == names.size()) {
			Env<Type> new_env = env;
			int index = 0;
			for (Type argType : types) {
				new_env = new ExtendEnv<Type>(new_env, names.get(index),
						argType);
				index++;
			}

			Type bodyType = (Type) e.body().accept(this, new_env);
			return new FuncT(types,bodyType);
		}
		return new ErrorT(message + ts.visit(e, null));
	}

	public Type visit(CallExp e, Env<Type> env) {
		List<Exp> operands = e.operands();

		Type opType = (Type) e.operator().accept(this, env);

		if (opType instanceof ErrorT) return opType;
		if (!(opType instanceof FuncT)) return new ErrorT("Expected a function type in the call expression, found "
				+ opType + " in " + ts.visit(e, null));
		
		List<Type> opTypes = ((FuncT) opType).argTypes();

		for(int i = 0; i < operands.size(); i++){
			Type type = (Type) operands.get(i).accept(this, env);
			Type operandType = opTypes.get(i);
			if(!(type.typeEqual(operandType))) return new ErrorT("The expected type of the " + i + "th actual" +
					"parameter is " + operandType + ", found " + type +" in "+ ts.visit(e, null));
		}

		return ((FuncT) opType).returnType();
	}

	public Type visit(LetrecExp e, Env<Type> env) {
		List<String> names = e.names();
		List<Type> types = e.types();
		List<Exp> fun_exps = e.fun_exps();

		// collect the environment
		Env<Type> new_env = env;
		for (int index = 0; index < names.size(); index++) {
			new_env = new ExtendEnv<Type>(new_env, names.get(index),
					types.get(index));
		}

		// verify the types of the variables
		for (int index = 0; index < names.size(); index++) {
			Type type = (Type)fun_exps.get(index).accept(this, new_env);

			if (type instanceof ErrorT) { return type; }

			if (!assignable(types.get(index), type)) {
				return new ErrorT("The expected type of the " + index +
						" variable is " + types.get(index).toString() +
						" found " + type.toString() + " in " +
						ts.visit(e, null));
			}
		}

		return (Type) e.body().accept(this, new_env);
	}

	public Type visit(IfExp e, Env<Type> env) {
		// answer question 5
		return new ErrorT("Not coded yet in IfExp.");
	}

	//Question 2a
	public Type visit(CarExp e, Env<Type> env) {
		Exp exp = e.arg();
		Type type = (Type)exp.accept(this, env);

		if (type instanceof ErrorT) return type;

		if (type instanceof PairT) return ((PairT) type).fst();

		return new ErrorT("The car expected an expression of type Pair, found "
		+ type.toString() + " in " + ts.visit(e, null));
	}

	public Type visit(CdrExp e, Env<Type> env) {
		Exp exp = e.arg();
		Type type = (Type)exp.accept(this, env);
		if (type instanceof ErrorT) { return type; }

		if (type instanceof PairT) {
			PairT pt = (PairT)type;
			return pt.snd();
		}

		return new ErrorT("The cdr expect an expression of type Pair, found "
				+ type.toString() + " in " + ts.visit(e, null));
	}

	public Type visit(ConsExp e, Env<Type> env) {
		Exp fst = e.fst(); 
		Exp snd = e.snd();

		Type t1 = (Type)fst.accept(this, env);
		if (t1 instanceof ErrorT) { return t1; }

		Type t2 = (Type)snd.accept(this, env);
		if (t2 instanceof ErrorT) { return t2; }

		return new PairT(t1, t2);
	}

	//Question 2b
	public Type visit(ListExp e, Env<Type> env) {
		Type type = e.type();
		List<Exp> elements = e.elems();
		int i = 0;
		for(Exp exp : elements){
			Type exptype = (Type)exp.accept(this, env);
			if (exptype instanceof ErrorT) return exptype;
			if (!exptype.typeEqual(type)) return new ErrorT("The " + i + " expression should have type "
			+ type + ", found " + exptype + " in " + ts.visit(e, null));
			i++;
		}
		return new ListT(type);
	}

	public Type visit(NullExp e, Env<Type> env) {
		Exp arg = e.arg();
		Type type = (Type)arg.accept(this, env);
		if (type instanceof ErrorT) { return type; }

		if (type instanceof ListT) { return BoolT.getInstance(); }

		return new ErrorT("The null? expect an expression of type List, found "
				+ type.toString() + " in " + ts.visit(e, null));
	}


	public Type visit(RefExp e, Env<Type> env) {
		Exp value = e.value_exp();
		Type type = e.type();
		Type expType = (Type)value.accept(this, env);
		if (type instanceof ErrorT) { return type; }

		if (expType.typeEqual(type)) {
			return new RefT(type);
		}

		return new ErrorT("The Ref expression expect type " + type.toString() +
				" found " + expType.toString() + " in " + ts.visit(e, null));
	}

	//Question 1a
	public Type visit(DerefExp e, Env<Type> env) {
		Exp exp = e.loc_exp();
		Type type = (Type)exp.accept(this, env);

		if (type instanceof ErrorT) { return type; }

		if (type instanceof RefT) { return ((RefT) type).nestType(); }

		return new ErrorT("The dereference expression expected a reference type, found " +
				 type.toString() + " in " + ts.visit(e, null));
	}

	//Question 1b
	public Type visit(AssignExp e, Env<Type> env) {
		Exp lhs = e.lhs_exp();
		Exp rhs = e.rhs_exp();
		Type ltype = (Type) lhs.accept(this, env);
		Type rtype = (Type) rhs.accept(this, env);

		if (ltype instanceof ErrorT) return ltype;

		if(ltype instanceof RefT) {
			if (rtype instanceof ErrorT) return rtype;
			if (rtype.typeEqual(ltype)) return rtype;
			return new ErrorT("The inner type of the reference type is "
			+ ((RefT) ltype).nestType().toString() + ", the rhs type is " + rtype.toString() + " in " + ts.visit(e, null) );
		}

		return new ErrorT("The lhs of the assignment expression expects a reference type, found "
				+ ltype.toString() + " in " + ts.visit(e, null));
	}

	public Type visit(FreeExp e, Env<Type> env) {
		Exp exp = e.value_exp();
		Type type = (Type)exp.accept(this, env);

		if (type instanceof ErrorT) { return type; }

		if (type instanceof RefT) { return UnitT.getInstance(); }

		return new ErrorT("The free expression expect a reference type " +
				"found " + type.toString() + " in " + ts.visit(e, null));
	}

	public Type visit(UnitExp e, Env<Type> env) {
		return Type.UnitT.getInstance();
	}

	public Type visit(NumExp e, Env<Type> env) {
		return NumT.getInstance();
	}

	public Type visit(StrExp e, Env<Type> env) {
		return Type.StringT.getInstance();
	}

	public Type visit(BoolExp e, Env<Type> env) {
		return Type.BoolT.getInstance();
	}

	public Type visit(LessExp e, Env<Type> env) {
		return visitBinaryComparator(e, env, ts.visit(e, null));
	}

	public Type visit(EqualExp e, Env<Type> env) {
		return visitBinaryComparator(e, env, ts.visit(e, null));
	}

	public Type visit(GreaterExp e, Env<Type> env) {
		return visitBinaryComparator(e, env, ts.visit(e, null));
	}


	private Type visitBinaryComparator(BinaryComparator e, Env<Type> env,
			String printNode) {
		// answer question 4
		return new ErrorT("Not coded yet in BinaryComparator.");
	}


	public Type visit(AddExp e, Env<Type> env) {
		return visitCompoundArithExp(e, env, ts.visit(e, null));
	}

	public Type visit(DivExp e, Env<Type> env) {
		return visitCompoundArithExp(e, env, ts.visit(e, null));
	}

	public Type visit(MultExp e, Env<Type> env) {
		return visitCompoundArithExp(e, env, ts.visit(e, null));
	}

	public Type visit(SubExp e, Env<Type> env) {
		return visitCompoundArithExp(e, env, ts.visit(e, null));
	}

	//Question 3
	private Type visitCompoundArithExp(CompoundArithExp e, Env<Type> env, String printNode) {
		// answer question 3
		return new ErrorT("Not coded yet in visitCompoundArithExp.");
	}

	private static boolean assignable(Type t1, Type t2) {
		if (t2 instanceof UnitT) { return true; }

		return t1.typeEqual(t2);
	}
	
	public Type visit(ReadExp e, Env<Type> env) {
		return UnitT.getInstance();
	}

	public Type visit(EvalExp e, Env<Type> env) {
		return UnitT.getInstance();
	}
}

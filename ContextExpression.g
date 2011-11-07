grammar ContextExpression;

options {
  language = Java;
}

@header {
package interdroid.contextdroid.contextexpressions;

import java.util.Map;
import java.util.HashMap;
}

@lexer::header {
package interdroid.contextdroid.contextexpressions;
}

@members {
public static final Expression parseExpression(final String expression) throws ExpressionParseException {
        if (expression == null || expression.trim().length() == 0)
            return null;

        CharStream stream = new ANTLRStringStream(expression);
        ContextExpressionLexer lexer = new ContextExpressionLexer(stream);
        TokenStream tokenStream = new CommonTokenStream(lexer);
        ContextExpressionParser parser = new ContextExpressionParser(tokenStream);
        try {
            return parser.context_expression();
        } catch (RecognitionException e) {
            throw new ExpressionParseException(e);
        }
}

public static final TypedValue parseTypedValue(final String expression) throws ExpressionParseException {
        if (expression == null || expression.trim().length() == 0)
            return null;

        CharStream stream = new ANTLRStringStream(expression);
        ContextExpressionLexer lexer = new ContextExpressionLexer(stream);
        TokenStream tokenStream = new CommonTokenStream(lexer);
        ContextExpressionParser parser = new ContextExpressionParser(tokenStream);
        try {
            return parser.typed_value();
        } catch (RecognitionException e) {
        	throw new ExpressionParseException(e);
        }
}

}

// Simple rules

configuration_options returns [Map<String, String> config]
	:	{config = new HashMap<String, String>();}
	(id=ID '=' val=RAW_VALUE) {config.put($id.getText(), $val.getText());}
	('&' id=ID '=' val=RAW_VALUE {config.put($id.getText(), $val.getText());})*
	;

value_path	returns [String path]
	:
		id=ID {StringBuffer buf = new StringBuffer($id.getText());}
		('/' id=ID {buf.append('/'); buf.append($id.getText());} )*
		{$path = buf.toString();}
	;

strategy	returns [Strategy strategy]
	:	'ALL' {$strategy = Strategy.ALL;}
	|	'ANY' {$strategy = Strategy.ANY;}
	;

comparator	returns [Comparator comp]
	:	'>' {$comp = Comparator.GREATER_THAN;}
	|	'<' {$comp = Comparator.LESS_THAN;}
	|	'>=' {$comp = Comparator.GREATER_THAN_OR_EQUALS;}
	|	'<=' {$comp = Comparator.LESS_THAN_OR_EQUALS;}
	|	'==' {$comp = Comparator.EQUALS;}
	|	'!=' {$comp = Comparator.NOT_EQUALS;}
	|	'regex' {$comp = Comparator.REGEX_MATCH;}
	|	'contains' {$comp = Comparator.STRING_CONTAINS;}
	;

binary_logic_operator	returns [LogicOperator value]
	:	'&&' {$value = LogicOperator.AND;}
	|	'||' {$value = LogicOperator.OR;}
	;

unary_logic_operator	returns [LogicOperator value]
	:	'!' {$value = LogicOperator.NOT;}
	;

math_operator	returns [MathOperator value]
	:	'+' {$value = MathOperator.PLUS;}
	|	'-' {$value = MathOperator.MINUS;}
	|	'*' {$value = MathOperator.TIMES;}
	|	'/' {$value = MathOperator.DIVIDE;}
	;

// Paser rules

typed_value	returns [TypedValue val]
	:	constant=constant_typed_value {$val = constant;}
	| 	context=context_typed_value {$val = context;}
	|	left_constant=constant_typed_value WS op=math_operator WS right=typed_value {$val = new CombinedTypedValue(left_constant, $op.value, right);}
	|	left_context=context_typed_value  WS op=math_operator WS right=typed_value {$val = new CombinedTypedValue(left_context, $op.value, right);}
	;

history_mode returns [HistoryReductionMode mode]
	:	'NONE' {$mode = HistoryReductionMode.NONE;}
	|	'MAX' {$mode = HistoryReductionMode.MAX;}
	|	'MIN' {$mode = HistoryReductionMode.MIN;}
	|	'MEAN' {$mode = HistoryReductionMode.MEAN;}
	|	'MEDIAN' {$mode = HistoryReductionMode.MEDIAN;}
	;

context_typed_value returns [ContextTypedValue val]
	:	entity=ID '/' path=value_path {$val = new ContextTypedValue(entity.getText(), path);}
	|	entity=ID '/' path=value_path '?' config=configuration_options {$val = new ContextTypedValue(entity.getText(), path, config);}
	|	entity=ID '/' path=value_path WS '{' WS mode=history_mode WS ',' WS time=INT WS '}' {$val = new ContextTypedValue(entity.getText(), path, mode, Long.parseLong(time.getText()));}
	|	entity=ID '/' path=value_path '?' config=configuration_options '{' WS mode=history_mode WS ',' WS time=INT WS '}' {$val = new ContextTypedValue(entity.getText(), path, config, mode, Long.parseLong(time.getText()));}

	;


constant_typed_value returns [ConstantTypedValue val]
	:	raw=RAW_VALUE {$val = new ConstantTypedValue(raw);}
	;

expression returns [Expression e]
	:	op=unary_logic_operator WS exp=expression {$e = new LogicExpression($op.value, exp);}
	| 	left=typed_value WS s=strategy WS c=comparator WS right=typed_value {$e = new ValueExpression(left, c, s, right);}
	| 	'(' leftExp=expression WS op=binary_logic_operator WS rightExp=expression ')' {$e = new LogicExpression(leftExp, $op.value, rightExp);}
	;

context_expression returns [Expression e]
	: exp=expression EOF {$e = exp;}
	;

// Lexar rules

fragment
RAW_VALUE returns [Object val]
	:	i=INT {$val = Long.parseLong($i.getText());}
	|	f=FLOAT {$val = Double.parseDouble($i.getText());}
	|	s=STRING {$val = String.valueOf($s.getText());}
	;

ID  :	('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*
    ;

INT :	'0'..'9'+
    ;

FLOAT
    :   ('0'..'9')+ '.' ('0'..'9')* EXPONENT?
    |   '.' ('0'..'9')+ EXPONENT?
    |   ('0'..'9')+ EXPONENT
    ;

WS  :   ( ' '
        | '\t'
        | '\r'
        | '\n'
        ) {$channel=HIDDEN;}
    ;

STRING
    :  '"' ( ESC_SEQ | ~('\\'|'"') )* '"'
    ;

fragment
EXPONENT : ('e'|'E') ('+'|'-')? ('0'..'9')+ ;

fragment
HEX_DIGIT : ('0'..'9'|'a'..'f'|'A'..'F') ;

fragment
ESC_SEQ
    :   '\\' ('b'|'t'|'n'|'f'|'r'|'\"'|'\''|'\\')
    |   UNICODE_ESC
    |   OCTAL_ESC
    ;

fragment
OCTAL_ESC
    :   '\\' ('0'..'3') ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7')
    ;

fragment
UNICODE_ESC
    :   '\\' 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
    ;

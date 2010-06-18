/**
 * amok
 *
 * Copyright 2008 Chris Brown
 *  - chrisincambo@gmail.com
 *  - http://chrisincambo.wordpress.com
 *
 * http://code.google.com/p/amok/
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc, 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

/**
 * Namespace
 */
var amok = {};

/**
 * Class to encapsulate the mock API within the DSL
 * READ THIS - The DSL set in amok.DOMAIN_SPECIFIC_LANGUAGE will always be used as a mixin for the mock object
 * THIS MEANS that the 'this' keyword will refer to the mock object NOT the objects own scope
 */
amok.DSL = {
    should_receive : function(method){
        //Add the method to the mock
        this.addMethod(new amok.Method(method, new amok.Expectation()));
        return this;
    },
    with_args: function(){
        var expectation = this.getLiveMethod().getExpectation();
        expectation.setArguments(arguments);
        this.getLiveMethod().setExpectation(expectation);
        return this;
    },
    times: function(integer){
        var expectation = this.getLiveMethod().getExpectation();
        expectation.setCallLimit(integer);
        this.getLiveMethod().setExpectation(expectation);
        return this;
    },
    and_return: function(){
        var expectation = this.getLiveMethod().getExpectation();
        expectation.setReturnValues(arguments);
        this.getLiveMethod().setExpectation(expectation);
        return this;        
    },
    and_callback: function(callback, message){
        var expectation = this.getLiveMethod().getExpectation();
        expectation.setCallback(callback, message);
        this.getLiveMethod().setExpectation(expectation);
        return this;        
    }
};

/**
 * Set to true if you wish amok to treat methods that start with an underscore as private
 */
amok.UNDERSCORE_EQUALS_PRIVATE = true;

/**
 * Here you can replace the default amok DSL with your own flavour
 */
amok.DOMAIN_SPECIFIC_LANGUAGE = amok.DSL;

/**
 * Will throw exceptions in the firebug console if set to true
 */
amok.FIREBUG_ERRORS = true;

/**
 * Will throw exceptionsin the browser if set to true
 * BUG - Firebug will sometimes catch browser exceptions and not display them (this is a firebug problem)
 */
amok.BROWSER_ERRORS = true;

/**
 * Static method to create the mock object
 */
amok.mock = function(mockee){
    var mock = new amok.Mock(mockee);
    return mock;
};

amok.Mock = function(mockee){
    var _mockee = mockee;
    var _methods = {};
    var _liveMethod = null

    //Mixin DSL
    var _dsl = amok.DOMAIN_SPECIFIC_LANGUAGE;
    for (var property in _dsl) if (!this[property]) this[property] = _dsl[property];

    //Add methods to the mock object
    this.addMethod = function(method){
        //Make sure the method is in the interface
        if(amok.Util.hasMethod(_mockee, method)){
            amok.Util.error("amok Method: "+ method +"() is not a public method within the interface of the object being mocked");
        }

        //Add the method to the mock
        this[method.getName()] = function(val){
            return method.callback(arguments);
        };      

        //Store the method in the methods array
        _methods[method.getName()] = method;
        _liveMethod = method;
    };

    //The last method to me added to the mock
    this.getLiveMethod = function(){
        return _liveMethod;
    }
};

/**
 * Class for the representation of mock methods
 * @param {String} name
 * @param {Object} expectation? - instance of amok.Expectation
 */
amok.Method = function(name, expectation){
    var _name = name;
    var _expectation = expectation;
    var _calls = 0;

    this.getName = function(){
        return _name;  
    };

    this.getExpectation = function(){
        return _expectation;
    };

    this.setExpectation = function(expectation){
        _expectation = expectation;
    };

    this.callback = function(args){
        //Set calls
        _calls++;

        //Check calls
        if (_expectation.getCallLimit() !== null && _calls > _expectation.getCallLimit()) amok.Util.error( "amok Method: "+ _name +"() - has exceeded its call limit of " + _expectation.getCallLimit() );

        //Check arguments
        for(var i = 0; i < _expectation.getArguments().length; i++){
            var arg = _expectation.getArguments()[i];
            var expectedArgBaseType = amok.Util.isBaseType(arg);
            var givenArgBaseType = args[i]._type || "Object"; //We haven't added _type to object as it will break for/in loops
        
            //Check the arguments has been passed
            if( !args[i] ){
                amok.Util.error( "amok Method: "+ _name +"() has been called without an expected argument - argument " + (i + 1) + " should have been " + arg );
            }

            //Do a type check if the expected argument is a base type
            if( expectedArgBaseType ){
                if( givenArgBaseType == expectedArgBaseType ){
                    continue;
                } else {
                    amok.Util.error( "amok Method: "+ _name +"() has been called with an argument that differs from that expected - actual value type: "+ givenArgBaseType +" should be: "+ expectedArgBaseType );                                       
                }
            }    

            //We have an argument and we're not checking type, so lets check equality
            if( !amok.Util.equal(arg, args[i]) ) {
                amok.Util.error("amok Method: "+ _name +"() has been called with an argument that differs from that expected - actual value: "+ args[i] +" should be: "+ arg);
            } 
        }

        //Fire callback
        if(_expectation.getCallback()) {
            _expectation.getCallback().callback(_expectation.getCallback().message); //dodgy looking syntax to execute the returned anonymous function
        }

        //Return value
        if(_expectation.getReturnValues().length > 0) {
            return _expectation.getReturnValues()[_calls - 1] || _expectation.getReturnValues()[_expectation.getReturnValues().length - 1];
        }
    };
};

/**
 * Object for storing method expectations
 */
amok.Expectation = function(){
    var _callLimit = null;
    var _arguments = [];
    var _returnValues = [];
    var _callback = null;

    this.getCallLimit = function(){
        return _callLimit;
    };

    this.setCallLimit = function(callLimit){
        _callLimit = callLimit;
    };

    this.getArguments = function(){
        return _arguments;
    };

    this.setArguments = function(args){
        _arguments = args;
    };

    this.getReturnValues = function(){
        return _returnValues;
    };

    this.setReturnValues = function(args){
        _returnValues = args;
    };

    this.getCallback = function(){
        return _callback;  
    };
    
    this.setCallback = function(callback, message){
        _callback = {callback: callback, message: message};
    }
  
};

amok.Util = {

    /**
     * Handles mock errors
     */
    error: function(msg){
        if(amok.FIREBUG_ERRORS && console.error) console.error('Exception: ' + msg);
        if(amok.BROWSER_ERRORS) throw msg;
    },

    /**
     * Compares the two items for equality,
     * Objects and Arrays are assesed based on properties not reference
     * @param a
     * @param b
     */
    equal: function(a, b){
        //Different type
        if (a._type != b._type) return false;

        //Check array contents
        if(a._type == "Array"){
            if (a.length != b.length) return false;
            for(var i = 0; i < a.length; i++){
                if ( !this.equal(a[i], b[i]) ) return false;
            }
            return true;
        }

        //Check object contents
        if(typeof a == "object"){
            for(property in a){
                if ( !b[property] || !this.equal(a[property], b[property]) ) return false;
            }
            for(property in b){
                if ( !a[property] || !this.equal(a[property], b[property]) ) return false;
            }            
            return true;
        }

        //Straight check
        return (a === b);
    },

    /**
     * Checks if the item is base type class
     * Returns the base type as a string or false
     * @param item
     */
    isBaseType: function(item){

        switch(item){
            case String:
                return "String";
            case Number:
                return "Number";
            case Array:
                return "Array";
            case Date:
                return "Date";
            case Boolean:
                return "Boolean";
            case Object:
                return "Object";
            case Function:
                return "Function";
            case RegExp:
                return "RegExp";
            default:
                return false;
        }
    },

    /**
     * Returns true if the object has the method
     * @param mokee
     * @param method
     */
    hasMethod: function(mockee, method){
        for(var property in mockee){
            if(property === method && typeof mockee[property] === "function") return true;
        }
        if(typeof mockee.prototype == "object"){
            for(var prototypeMethod in mockee.prototype){
               if(prototypeMethod === method && typeof mockee.prototype[prototypeMethod] == "function") return true;
            }
            var constructorMethods = this.getConstructorMethods(mockee);
            for(var i = 0; i < constructorMethods.length; i++){
                if(constructorMethods[i] === method) return true;
            }
        }
        return false;
    },

    /**
     * Returns all of the methods within the constructor without the need for instantiation
     */
    getConstructorMethods: function(classObject){
        var constructorString = classObject.prototype.constructor.toString();
        var pattern = /this\..+\s*\=\s*\(*\s*function\s*\(\)/g;
        var result;
        var methods = [];

        while((result = pattern.exec(constructorString)) != null) {

            var tmp = constructorString.substring(0, result.index);

            var leftCurlyBracket = /\{/g;
            var rightCurlyBracket  = /\}/g;

            var i=0;

            while(leftCurlyBracket.exec(tmp) != null) {
                i++;
            }

            var j=0;
            while(rightCurlyBracket.exec(tmp) != null) {
                j++;
            }

            if (i - 1 == j) methods.push(result[0].substring(5, result[0].indexOf("=")).replace(" ", ""));
        }

        return methods;
    }
};

/**
 * Yes I know this is evil but I've built this for use with jsspec and they're already doing it
 */
String.prototype._type = "String";
Number.prototype._type = "Number";
Date.prototype._type = "Date";
Array.prototype._type = "Array";
Boolean.prototype._type = "Boolean";
RegExp.prototype._type = "RegExp";
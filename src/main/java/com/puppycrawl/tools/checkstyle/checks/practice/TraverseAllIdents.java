///////////////////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code and other text files for adherence to a set of rules.
// Copyright (C) 2001-2024 the original author or authors.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
///////////////////////////////////////////////////////////////////////////////////////////////

package com.puppycrawl.tools.checkstyle.checks.practice;


import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.checks.practice.api.Bigram;
import com.puppycrawl.tools.checkstyle.checks.practice.api.SuggestItem;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * <div>
 * Maintains a set of check suppressions from {@code @SuppressWarnings} annotations.
 * It allows to prevent Checkstyle from reporting violations from parts of code that were
 * annotated with {@code @SuppressWarnings} and using name of the check to be excluded.
 * It is possible to suppress all the checkstyle warnings with the argument {@code "all"}.
 * You can also use a {@code checkstyle:} prefix to prevent compiler
 * from processing these annotations.
 * You can also define aliases for check names that need to be suppressed.
 * </div>
 *
 * <ul>
 * <li>
 * Property {@code aliasList} - Specify aliases for check names that can be used in code
 * within {@code SuppressWarnings} in a format of comma separated attribute=value entries.
 * The attribute is the fully qualified name of the Check and value is its alias.
 * Type is {@code java.lang.String[]}.
 * Default value is {@code ""}.
 * </li>
 * </ul>
 *
 * <p>
 * Parent is {@code com.puppycrawl.tools.checkstyle.TreeWalker}
 * </p>
 *
 * @since 5.7
 */
public class TraverseAllIdents extends AbstractCheck {


    SymSpell symSpell ;
    int maxEditDistance = 2;
    boolean includeUnknowns = false;
    // I don't know correct way of initializing objects in this project
    // so for now initializing with default way
    {
        try {

            Map<Bigram, Long> bigrams = Files.lines(new File("/home/bluelord/Opensource/checkstyle/src/main/java/com/puppycrawl/tools/checkstyle/checks/practice/bigrams").toPath())
                    .map(line -> line.split(" "))
                    .collect(Collectors.toMap(tokens -> new Bigram(tokens[0], tokens[1]), tokens -> Long.parseLong(tokens[2])));
            Map<String, Long> unigrams = Files.lines(new File("/home/bluelord/Opensource/checkstyle/src/main/java/com/puppycrawl/tools/checkstyle/checks/practice/wordsdict").toPath())
                    .map(line -> line.split(","))
                    .collect(Collectors.toMap(tokens -> tokens[0], tokens -> Long.parseLong(tokens[1])));
            symSpell = new SymSpellBuilder().setUnigramLexicon(unigrams)
                    .setBigramLexicon(bigrams)
                    .setMaxDictionaryEditDistance(2)
                    .createSymSpell();
        }catch (Exception e){
            System.out.println("caught initialization exception "+ e);
        }
    }

    @Override
    public int[] getDefaultTokens() {
        return getRequiredTokens();
    }

    @Override
    public int[] getAcceptableTokens() {
        return getRequiredTokens();
    }
    //targeting all identifiers for now,
    // need to add properties for user specific requirement
    @Override
    public int[] getRequiredTokens() {
        return new int[]{
            TokenTypes.METHOD_DEF,
            TokenTypes.VARIABLE_DEF,
            TokenTypes.CLASS_DEF,
            TokenTypes.INTERFACE_DEF,
            TokenTypes.PARAMETER_DEF,
            TokenTypes.PACKAGE_DEF,
        };
    }
    //recursive search
    @Override
    public void visitToken(DetailAST ast) {


        final String currWord = ast
                .findFirstToken(TokenTypes.IDENT)
                .getText();
        try {

            if(isAcceptableWord(currWord)){
                // assuming words are either all small case,
                //camelcase or pascal case
                // and used regex to split that word
                // since target language is java, snake_case is not our concern
                //as other checks will take care of it
                String[] words=currWord.split("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])");//("(?<!^)(?=[A-Z])");
                for(String word : words){
                    if(word.length() == 1){
                        continue;
                    }
                    //System.out.println(word);
                    word=word.toLowerCase();
                    List<SuggestItem> suggestions = symSpell.lookup(word,Verbosity.ALL,false);

                    if(suggestions.size() == 0){
                        continue;
                    }
                    //right now only limiting to print 1 suggestion.
                    //can be user-specific based on props
                    String bestSuggestion=suggestions.get(0).getSuggestion();
                    if(!Objects.equals(bestSuggestion, word)) {
                        final String message = "Mispelled word in "
                                + currWord
                                + " \nDid you meant - "
                                + bestSuggestion
                                + " instead of - "
                                + word;
                        log(ast, message);
                    }
                }

            }

        } catch(Exception e){
            System.out.println(e);
        }

        super.visitToken(ast);
    }

    //Will apply checks only on words that consists only of letters
    private boolean isAcceptableWord(String currWord) {
        if(currWord == null || currWord.isEmpty()){
            return false;
        }

        for(int i=1;i<currWord.length();i++){
            if(!Character.isLetter(currWord.charAt(i))){
                return false;
            }
        }
        return true;
    }


}

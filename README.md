# README #

This is the README file for the CCG Induction, models, CCGbank Simplification, etc in 

*Induction*

    Y. Bisk and J. Hockenmaier, “Simple Robust Grammar Induction with Combinatory Categorial Grammars,” in Proceedings of 
    the Twenty-Sixth Conference on Artificial Intelligence (AAAI-12), Toronto, Canada, July 2012, pp. 1643–1649

*Model*

    Y. Bisk and J. Hockenmaier, “An HDP Model for Inducing Combinatory Categorial Grammars,” Transactions of the Association    for Computational Linguistics, pp. 75–88, 2013.

*CCGbank Simplification/Evaluation*

    Y. Bisk and J. Hockenmaier, “Probing the linguistic strengths and limitations of unsupervised grammar induction,” in Proceedings of the 53rd Annual Meeting of the Association for Computational Linguistics (Volume 1: Long Papers), Beijing,China, July 2015


Please don't hesitate to contact me or file bugs.  I have tried to clean up the code, but may have introduced bugs or deleted something important in the process.  Thanks!

### Checkout the code ###
```
git clone https://github.com/ybisk/CCG-Induction.git
```

### Compiling Maven code ###
* [Download and Install Maven](http://maven.apache.org/download.cgi)
* Move into git repo:  ```cd CCG-Induction/ ```
* Building a jar file: ```mvn package [-DskipTests]```

All the classes (and files under resources) are in ```target/```

### Running the code ###
```java -jar target/CCGInduction-1.0-jar-with-dependencies.jar```


### FAQ ###
[Wiki](https://github.com/ybisk/CCG-Induction/wiki)

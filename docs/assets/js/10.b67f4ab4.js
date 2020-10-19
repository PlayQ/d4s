(window.webpackJsonp=window.webpackJsonp||[]).push([[10],{362:function(t,a,s){"use strict";s.r(a);var e=s(42),n=Object(e.a)({},(function(){var t=this,a=t.$createElement,s=t._self._c||a;return s("ContentSlotsDistributor",{attrs:{"slot-key":t.$parent.slotKey}},[s("h1",{attrs:{id:"basic-queries"}},[s("a",{staticClass:"header-anchor",attrs:{href:"#basic-queries"}},[t._v("#")]),t._v(" Basic queries")]),t._v(" "),s("p",[s("strong",[t._v("D4S")]),t._v(" has a rich support of almost all DynamoDB operations. We will start with the most basic ones:")]),t._v(" "),s("ul",[s("li",[t._v("getItem - retrieves a single item from a table.")]),t._v(" "),s("li",[t._v("putItem -  adds a new item or replaces existing one.")]),t._v(" "),s("li",[t._v("updateItem - updates an existing item.")]),t._v(" "),s("li",[t._v("deleteItem - removes an item from a table.")]),t._v(" "),s("li",[t._v("scan - fetches all elements in a table.")]),t._v(" "),s("li",[t._v("query - fetches all elements of a table that match some criteria using hash/range keys.")])]),t._v(" "),s("p",[t._v("To run any query that built using D4S you must use "),s("code",[t._v("DynamoConnector")]),t._v(". Say we have a variable named "),s("code",[t._v("connector")]),t._v(" of "),s("code",[t._v("DynamoConnector")]),t._v(" type,\nthen typical call to the database would look like this:")]),t._v(" "),s("div",{staticClass:"language-scala extra-class"},[s("pre",{pre:!0,attrs:{class:"language-scala"}},[s("code",[t._v("connector"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("run"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("(")]),s("span",{pre:!0,attrs:{class:"token string"}},[t._v('"query name"')]),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(")")]),t._v(" "),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("{")]),t._v("\n  "),s("span",{pre:!0,attrs:{class:"token comment"}},[t._v("// query itself.")]),t._v("\n"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("}")]),t._v("\n")])])]),s("p",[t._v("Remember our example with leaderboard service? Let's implement "),s("code",[t._v("Ladder")]),t._v(" interface and see how we could use one of the listed above queries\nto interact with DB. If you forget how "),s("code",[t._v("Ladder")]),t._v(" interface looks like, here is a quick reminder:")]),t._v(" "),s("div",{staticClass:"language-scala extra-class"},[s("pre",{pre:!0,attrs:{class:"language-scala"}},[s("code",[s("span",{pre:!0,attrs:{class:"token keyword"}},[t._v("trait")]),t._v(" Ladder"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("[")]),t._v("F"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("[")]),t._v("_"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(",")]),t._v(" _"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("]")]),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("]")]),t._v(" "),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("{")]),t._v("\n  "),s("span",{pre:!0,attrs:{class:"token keyword"}},[t._v("def")]),t._v(" submitScore"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("(")]),t._v("userId"),s("span",{pre:!0,attrs:{class:"token operator"}},[t._v(":")]),t._v(" UserId"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(",")]),t._v(" score"),s("span",{pre:!0,attrs:{class:"token operator"}},[t._v(":")]),t._v(" Score"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(")")]),s("span",{pre:!0,attrs:{class:"token operator"}},[t._v(":")]),t._v(" F"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("[")]),t._v("QueryFailure"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(",")]),t._v(" "),s("span",{pre:!0,attrs:{class:"token builtin"}},[t._v("Unit")]),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("]")]),t._v("\n  "),s("span",{pre:!0,attrs:{class:"token keyword"}},[t._v("def")]),t._v(" getScores"),s("span",{pre:!0,attrs:{class:"token operator"}},[t._v(":")]),t._v(" F"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("[")]),t._v("QueryFailure"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(",")]),t._v(" List"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("[")]),t._v("UserWithScore"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("]")]),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("]")]),t._v("\n"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("}")]),t._v("\n")])])]),s("p",[t._v("Typical implementation of the persistence layer using D4S could look like this:")]),t._v(" "),s("div",{staticClass:"language-scala extra-class"},[s("pre",{pre:!0,attrs:{class:"language-scala"}},[s("code",[s("span",{pre:!0,attrs:{class:"token keyword"}},[t._v("final")]),t._v(" "),s("span",{pre:!0,attrs:{class:"token keyword"}},[t._v("class")]),t._v(" D4SLadder"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("[")]),t._v("F"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("[")]),s("span",{pre:!0,attrs:{class:"token operator"}},[t._v("+")]),t._v("_"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(",")]),t._v(" "),s("span",{pre:!0,attrs:{class:"token operator"}},[t._v("+")]),t._v("_"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("]")]),s("span",{pre:!0,attrs:{class:"token operator"}},[t._v(":")]),t._v(" Bifunctor2"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("]")]),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("(")]),t._v("connector"),s("span",{pre:!0,attrs:{class:"token operator"}},[t._v(":")]),t._v(" DynamoConnector"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("[")]),t._v("F"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("]")]),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(",")]),t._v(" ladderTable"),s("span",{pre:!0,attrs:{class:"token operator"}},[t._v(":")]),t._v(" LadderTable"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(")")]),t._v(" "),s("span",{pre:!0,attrs:{class:"token keyword"}},[t._v("extends")]),t._v(" Ladder"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("[")]),t._v("F"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("]")]),t._v(" "),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("{")]),t._v("\n  "),s("span",{pre:!0,attrs:{class:"token keyword"}},[t._v("import")]),t._v(" "),s("span",{pre:!0,attrs:{class:"token namespace"}},[t._v("ladderTable"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")])]),t._v("_\n\n  "),s("span",{pre:!0,attrs:{class:"token keyword"}},[t._v("override")]),t._v(" "),s("span",{pre:!0,attrs:{class:"token keyword"}},[t._v("def")]),t._v(" getScores"),s("span",{pre:!0,attrs:{class:"token operator"}},[t._v(":")]),t._v(" F"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("[")]),t._v("QueryFailure"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(",")]),t._v(" List"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("[")]),t._v("UserWithScore"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("]")]),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("]")]),t._v(" "),s("span",{pre:!0,attrs:{class:"token operator"}},[t._v("=")]),t._v(" "),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("{")]),t._v("\n    connector\n      "),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("run"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("(")]),s("span",{pre:!0,attrs:{class:"token string"}},[t._v('"get scores query"')]),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(")")]),t._v(" "),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("{")]),t._v("\n        table"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("scan"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("decodeItems"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("[")]),t._v("UserIdWithScoreStored"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("]")]),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("execPagedFlatten"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("(")]),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(")")]),t._v("\n      "),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("}")]),t._v("\n      "),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("leftMap"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("(")]),t._v("err "),s("span",{pre:!0,attrs:{class:"token keyword"}},[t._v("=>")]),t._v(" QueryFailure"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("(")]),t._v("err"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("message"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(",")]),t._v(" err"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("cause"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(")")]),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(")")]),t._v("\n      "),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("map"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("(")]),t._v("_"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("map"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("(")]),t._v("_"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("toAPI"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(")")]),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(")")]),t._v("\n  "),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("}")]),t._v("\n\n  "),s("span",{pre:!0,attrs:{class:"token keyword"}},[t._v("override")]),t._v(" "),s("span",{pre:!0,attrs:{class:"token keyword"}},[t._v("def")]),t._v(" submitScore"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("(")]),t._v("userId"),s("span",{pre:!0,attrs:{class:"token operator"}},[t._v(":")]),t._v(" UserId"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(",")]),t._v(" score"),s("span",{pre:!0,attrs:{class:"token operator"}},[t._v(":")]),t._v(" Score"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(")")]),s("span",{pre:!0,attrs:{class:"token operator"}},[t._v(":")]),t._v(" F"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("[")]),t._v("QueryFailure"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(",")]),t._v(" "),s("span",{pre:!0,attrs:{class:"token builtin"}},[t._v("Unit")]),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("]")]),t._v(" "),s("span",{pre:!0,attrs:{class:"token operator"}},[t._v("=")]),t._v(" "),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("{")]),t._v("\n    connector\n      "),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("run"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("(")]),s("span",{pre:!0,attrs:{class:"token string"}},[t._v('"submit user\'s score"')]),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(")")]),t._v(" "),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("{")]),t._v("\n        table"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("updateItem"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("(")]),t._v("UserIdWithScoreStored"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("(")]),t._v("userId"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("value"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(",")]),t._v(" score"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("value"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(")")]),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(")")]),t._v("\n      "),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("}")]),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("leftMap"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("(")]),t._v("err "),s("span",{pre:!0,attrs:{class:"token keyword"}},[t._v("=>")]),t._v(" QueryFailure"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("(")]),t._v("err"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("message"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(",")]),t._v(" err"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("cause"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(")")]),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(")")]),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("void\n  "),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("}")]),t._v("\n"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("}")]),t._v("\n")])])]),s("p",[t._v("A lot of things happening here, but don't worry we'll explain everything in a bit. "),s("code",[t._v("D4SLadder")]),t._v(" constructor requires two parameters: "),s("code",[t._v("DynamoConnector")]),t._v("\nto run a query and "),s("code",[t._v("LadderTable")]),t._v(" which is our "),s("RouterLink",{attrs:{to:"/docs/table-definition.html"}},[t._v("table definition")]),t._v(". We also require and instance of "),s("code",[t._v("Bifunctor2")]),t._v(" from Izumi for "),s("code",[t._v("leftMap")]),t._v(".")],1),t._v(" "),s("h2",{attrs:{id:"scan-and-query"}},[s("a",{staticClass:"header-anchor",attrs:{href:"#scan-and-query"}},[t._v("#")]),t._v(" Scan and Query")]),t._v(" "),s("p",[t._v("In order to retrieve scores, we need to scan the whole ladder table. All queries are implemented as extension methods for "),s("code",[t._v("TableReferece")]),t._v(" data type.\nSo to build a query you need to use "),s("code",[t._v("table")]),t._v(" value from "),s("code",[t._v("LadderTable")]),t._v(" as we described here:")]),t._v(" "),s("div",{staticClass:"language-scala extra-class"},[s("pre",{pre:!0,attrs:{class:"language-scala"}},[s("code",[t._v("table"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("scan"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("decodeItems"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("[")]),t._v("UserIdWithScoreStored"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("]")]),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("execPagedFlatten"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("(")]),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(")")]),t._v("\n")])])]),s("p",[t._v("This query simply scans the table and decodes items (using previously defined codecs). Okay, but why we need this "),s("code",[t._v(".execPagedFlatten()")]),t._v(" combinator?\nWe could have a huge number of records in the table that couldn't fit in one page of scan result. Using "),s("code",[t._v("execPagedFlatten")]),t._v(" we create a query\nthat handles pagination and flattens all pages into a single one-dimensional list of items. What if we'll change our interface and add one more method\nto fetch records with users that have a score greater or equal to 42. This is how such a query could be expressed with D4S:")]),t._v(" "),s("div",{staticClass:"language-scala extra-class"},[s("pre",{pre:!0,attrs:{class:"language-scala"}},[s("code",[s("span",{pre:!0,attrs:{class:"token keyword"}},[t._v("import")]),t._v(" "),s("span",{pre:!0,attrs:{class:"token namespace"}},[t._v("d4s"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("implicits"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")])]),t._v("_\ntable\n  "),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("query"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("(")]),t._v("mainFullKey"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("(")]),t._v("userId"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(")")]),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(")")]),t._v("\n  "),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("withFilterExpression"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("(")]),s("span",{pre:!0,attrs:{class:"token string"}},[t._v('"score"')]),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("of"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("[")]),s("span",{pre:!0,attrs:{class:"token builtin"}},[t._v("Long")]),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("]")]),t._v(" "),s("span",{pre:!0,attrs:{class:"token operator"}},[t._v(">=")]),t._v(" "),s("span",{pre:!0,attrs:{class:"token number"}},[t._v("42")]),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(")")]),t._v("\n  "),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("decodeItems"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("[")]),t._v("UserIdWithScoreStored"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("]")]),t._v("  \n  "),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("execPagedFlatten"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("(")]),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(")")]),t._v("\n")])])]),s("p",[t._v("Here we use "),s("code",[t._v("query")]),t._v(" operation that requires at least one table's key, we pass the user's id. You've already known about "),s("code",[t._v("decodeItems")]),t._v(" and\n"),s("code",[t._v("execPagedFlatten")]),t._v(", but this "),s("code",[t._v("withFilterExpression")]),t._v(" combinator is something new. This combinator applies a filter on a query.\nThe filter requires a "),s("code",[t._v("Condition")]),t._v(" which could be build using implicit methods from "),s("code",[t._v("d4s.implicits")]),t._v(" object. The "),s("code",[t._v("of")]),t._v(" method specify type of the attribute\nwe wanna use in an expression. In our case we use score attribute and tell D4S that it has type of Long, then using method "),s("code",[t._v(">=")]),t._v(" compare it with a particular value.\nFor more information about conditionals, please refer to "),s("RouterLink",{attrs:{to:"/docs/conditionals.html"}},[t._v("Conditionals")]),t._v(" page.")],1),t._v(" "),s("h2",{attrs:{id:"put-update-and-delete"}},[s("a",{staticClass:"header-anchor",attrs:{href:"#put-update-and-delete"}},[t._v("#")]),t._v(" Put, Update and Delete")]),t._v(" "),s("p",[t._v("Now, let's look at "),s("code",[t._v("submitScore")]),t._v(" method. The best way to put data or update it if it's already in the table using "),s("code",[t._v("updateItem")]),t._v(" query.\nAll you need is to pass the data you want to star into "),s("code",[t._v("updateItem")]),t._v(" combinator.")]),t._v(" "),s("div",{staticClass:"language-scala extra-class"},[s("pre",{pre:!0,attrs:{class:"language-scala"}},[s("code",[t._v("table"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("updateItem"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("(")]),t._v("UserIdWithScoreStored"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("(")]),t._v("userId"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("value"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(",")]),t._v(" score"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("value"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(")")]),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(")")]),t._v("\n")])])]),s("p",[t._v("Put operation won't differ from update too much:")]),t._v(" "),s("div",{staticClass:"language-scala extra-class"},[s("pre",{pre:!0,attrs:{class:"language-scala"}},[s("code",[t._v("table"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("putItem"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("(")]),t._v("UserIdWithScoreStored"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("(")]),t._v("userId"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("value"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(",")]),t._v(" score"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("value"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(")")]),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(")")]),t._v("\n")])])]),s("p",[t._v("And the last, but not least is delete operation. D4S has a "),s("code",[t._v("deleteItem")]),t._v(" method that takes a table's key and removes and item.\nLet's use it to remove a particular score from the ladder:")]),t._v(" "),s("div",{staticClass:"language-scala extra-class"},[s("pre",{pre:!0,attrs:{class:"language-scala"}},[s("code",[t._v("table"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(".")]),t._v("deleteItem"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("(")]),t._v("mainFullKey"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v("(")]),t._v("userId"),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(")")]),s("span",{pre:!0,attrs:{class:"token punctuation"}},[t._v(")")]),t._v("\n")])])]),s("p",[t._v("Okay, in this section we finally discovered how to make queries with D4S and before we go to know for other operation and possibilities\nthat D4S provides, we would like to highlight what we've learnt so far:")]),t._v(" "),s("ul",[s("li",[t._v("we able to setup project with D4S")]),t._v(" "),s("li",[t._v("we able to define a table")]),t._v(" "),s("li",[t._v("we able to perform basic operations on DynamoDB.")])]),t._v(" "),s("p",[t._v("Good job, and see ya in the next chapter!")])])}),[],!1,null,null,null);a.default=n.exports}}]);
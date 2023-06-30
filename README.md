# Algorithms to Visualize Geophylogenies

This repository contains the implementation of algorithms described and tested in the paper:

**Visualizing Geophylogenies – Internal and External Labeling with Phylogenetic Tree Constraints** *Jonathan Klawitter, Felix Klesen, Joris Y. Scholl, Thomas C. van Dijk and Alexander Zaft*.

The paper focuses on visualizing geophylogenies, which are phylogenetic trees where each biological taxon (leaf) is associated with a geographic location (site). 

## Geophylogenies and Visualization

A **geophylogeny** is a phylogenetic tree where each leaf (biological taxon) has an associated geographic location (site). To clearly visualize a geophylogeny, the tree is typically represented as a crossing-free drawing next to a map. There are two common approaches for showing the correspondence between taxa and sites:

1. **Internal Labeling**: In this approach, matching labels are placed on the map itself to indicate the association between sites and taxa.

2. **External Labeling**: In this approach, **leaders** are used to connect each site to the corresponding leaf of the tree, visually demonstrating the association.

The order of the leaves in the tree plays a crucial role in understanding the relationship between sites and taxa. This repository provides algorithms and tools to optimize the visualization of geophylogenies based on various quality measures for internal labeling. It also includes algorithms and heuristics for minimizing the number of leader crossings in external labeling, which is an NP-hard problem. 

## Repository Contents

This repository is divided into two main parts: a Java implementation for optimal internal labeling and heuristics for external labeling, and a Python + Gurobi implementation of the integer linear program (ILP) to optimally solve external labeling.
Both include functions to output the drawings of geophylogenies with the computed leaf orders as SVG.

### Java Implementation

The Java implementation includes the following components:

- **Exact Algorithms for Internal Labeling**: Efficient DP algorithm to optimize the quality measures for internal labeling.
- **Heuristics for External Labeling**: Heuristic algorithms to minimize leader crossings in external labeling.
- **Test Example Generation**: Code to generate test examples for evaluation and experimentation.

### Python Implementation

The Python implementation solvies the external labeling problem using Gurobi as an integer linear programming (ILP) solver. 


### Additional Resources

In addition to the code to generate realistic synthetic instances, we have included several real-world examples used in our experiments. 
If you utilize these examples in your work, we request that you cite the respective references mentioned in our paper.

## License

The code in this repository is licensed under the [MIT License](LICENSE). Feel free to use and modify the code according to the terms of the license; this README file is Creative Commons CC-BY licensed.

## Acknowledgments 

If you use this repository or find it helpful in your research or project, please consider citing the paper:

> Jonathan Klawitter, Felix Klesen, Joris Y. Scholl, Thomas C. van Dijk, and Alexander Zaft. 
> Visualizing Geophylogenies – Internal and External Labeling with Phylogenetic Tree Constraints. 
> *Geographic Information Science (GIScience)*, LIPIcs, 2023. (to appear)

## Help

If you have any questions or need assistance with the code, please feel free to contact the authors *JK* or *TvD*. We'd be happy to help. 

In particular, if you want to run the algorithms on other trees in NEXUS format, we have a fragile parser to create a tree of this model. 

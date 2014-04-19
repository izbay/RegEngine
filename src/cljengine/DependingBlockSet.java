
        public DependingBlockSet doRevDepsSearch()
        {
                final DependingBlockSet sIn = this;
                final DependingBlockSet sOut = new DependingBlockSet(sIn);
                final DependingBlockSet sSearch = new DependingBlockSet(sIn);

                while(!sIn.isEmpty())
                {
                        final Iterator<DependingBlock> it = sSearch.blocks.values().iterator();
                        final DependingBlock b = it.next();
                        it.remove();

                        final DependingBlockSet sD = b.allRevDependencies();
                        for(BlockVector vDep : sD.blocks.keySet())
                        {
                                final DependingBlock dDep = sD.blocks.get(vDep);
                                assert(!dDep.action.isHardDependency());
                                if(!sSearch.contains(vDep))
                                {
                                        sSearch.add(dDep);
                                        if(!sOut.contains(vDep))
                                        {	sOut.add(dDep);	}// if
                                }// if
                                else
                                {
                                        assert(!sOut.contains(vDep));
                                }// else
                        }// for
                }// while

                return sOut;
    }// doRevDepsSearch()
